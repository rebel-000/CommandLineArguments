package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.github.rebel000.cmdlineargs.extensions.PlatformExtension
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectReader
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectWriter
import com.github.rebel000.cmdlineargs.serialization.xml.XmlObjectReader
import com.github.rebel000.cmdlineargs.serialization.xml.XmlObjectWriter
import com.github.rebel000.cmdlineargs.tree.*
import com.github.rebel000.cmdlineargs.tree.visitors.CollectArgsVisitor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.multilaunch.MultiLaunchConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.io.copy
import kotlinx.coroutines.*
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class ArgumentsService(val project: Project, private val cs: CoroutineScope) : Disposable, TreeModelListener {
    companion object {
        const val SERIALIZE_REVISION: Int = 3
        val DEFERRED_SAVE_DELAY: Duration = 1.seconds

        fun getInstance(project: Project): ArgumentsService = project.service()
        fun getInstanceIfCreated(project: Project?): ArgumentsService? = project?.serviceIfCreated()
    }

    private val _isArgumentsInvalid = AtomicBoolean(false)
    private val _isPreviewInvalid = AtomicBoolean(false)

    @Volatile
    private var _commonArguments: String = ""
    @Volatile
    private var _isEnabled = true
    @Volatile
    private var _isRunManagerLoaded = false
    @Volatile
    private var _isSharedEnabled = false
    @Volatile
    private var _revision = -1
    @Volatile
    private var _saveJob: Job? = null

    private data class SettingsData(val adapter: ArgumentsAdapter?, val node: ConfigurationNode)

    private val _model = ArgumentTreeModel()
    private val perSettingsData = ConcurrentHashMap<String, SettingsData>()
    private val globalStorage: ArgumentsGlobalStorage.State get() = ArgumentsGlobalStorage.getInstance().state
    private val projectStorage: ArgumentsProjectStorage.State get() = ArgumentsProjectStorage.getInstance(project).state
    private var stateFilePath: String? = null

    internal val model: ArgumentTreeModel get() {
        ApplicationManager.getApplication().assertIsDispatchThread()
        return _model
    }

    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled != value) {
                _isEnabled = value
                onEnabledChanged()
            }
        }

    var showSharedArguments
        get() = _isSharedEnabled
        set(value) {
            if (_isSharedEnabled != value) {
                _isSharedEnabled = value
                globalStorage.showSharedNode = value
                onShowSharedChanged()
            }
        }

    var showHidden
        get() = globalStorage.showHidden
        set(value) {
            globalStorage.showHidden = value
            invalidate(preview = true)
        }

    var showExperimental
        get() = globalStorage.showExperimental
        set(value) {
            globalStorage.showExperimental = value
            invalidate(preview = true)
        }

    var showNotSupported
        get() = globalStorage.showUnsupported
        set(value) {
            globalStorage.showUnsupported = value
            invalidate(preview = true)
        }

    val revision: Int get() = _revision

    init {
        _model.addTreeModelListener(this)
        ApplicationManager.getApplication().invokeLater {
            reload()
            invalidate(arguments = true)
        }
    }

    override fun dispose() {
        _saveJob?.cancel()
        save()
    }

    fun getAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return perSettingsData[s.uniqueID]?.let { (adapter, node) ->
            adapter?.takeIf { it.settings === s && node.visible && it.isTrusted() }
        }
    }

    fun getArguments(s: RunnerAndConfigurationSettings): String {
        return getAdapter(s)?.getArguments() ?: ""
    }

    fun getFilters(): List<FilterDefinition> {
        val customFilters = PlatformExtension.EP_NAME.extensionList.firstNotNullOfOrNull { it.getFilters(project) }
        if (customFilters != null) {
            return customFilters
        }
        return listOf(
            FilterDefinition(
                "runConfiguration",
                Messages.message("properties.runConfigurationFilters"),
                Messages.message("properties.runConfigurationFilters.desc"),
                RunManager.getInstanceIfCreated(project)
                    ?.allSettings
                    .orEmpty()
                    .mapNotNull { 
                        it.takeIf { getAdapter(it) != null }?.getQualifiedFilterName()
                    }.distinct()
            )
        )
    }

    internal fun findAdapter(uniqueID: String): ArgumentsAdapter? {
        return perSettingsData[uniqueID]?.adapter
    }

    internal fun isTypeTrusted(typeID: String): Boolean {
        return projectStorage.trustedConfigTypes.contains(typeID)
    }

    internal fun setTypeTrusted(typeID: String, trusted: Boolean) {
        if (trusted) {
            projectStorage.trustedConfigTypes.add(typeID)
        } else {
            projectStorage.trustedConfigTypes.remove(typeID)
        }
        invalidate(preview = true)
    }

    internal fun onConfigurationsVisibilityChanged(invalidateTrust: Boolean) {
        if (invalidateTrust) {
            perSettingsData.forEach { (_, it) ->
                val trusted = it.adapter?.isTrusted() ?: false
                if (it.node.trusted != trusted) {
                    it.node.trusted = trusted
                    model.invalidate(it.node, false)
                }
            }
        }
        invalidate(preview = true)
    }

    internal fun onProcessStarting(env: ExecutionEnvironment) {
        val settings = env.runnerAndConfigurationSettings
        if (isEnabled && settings != null) {
            val adapter = getAdapter(settings)
            if (adapter != null) {
                adapter.fireOnStart()
            }
            else if (env.isRunningCurrentFile) {
                createAdapter(settings, true)
                    ?.takeIf { it.isTrusted() }
                    ?.let { adapter ->
                        adapter.setArguments(_commonArguments)
                        adapter.fireOnStart()
                    }
            }
        }
    }

    internal fun onProcessNotStarted(env: ExecutionEnvironment) {
        val settings = env.runnerAndConfigurationSettings
        if (isEnabled && settings != null) {
            getAdapter(settings)?.fireOnCleanup()
        }
    }

    internal fun onProcessStarted(env: ExecutionEnvironment) {
        val settings = env.runnerAndConfigurationSettings
        if (isEnabled && settings != null) {
            getAdapter(settings)?.fireOnCleanup()
        }
    }

    internal fun onRunConfigurationAdded(s: RunnerAndConfigurationSettings) {
        if (s.configuration is CompoundRunConfiguration || s.configuration is MultiLaunchConfiguration) {
            return
        }
        val uniqueID = s.uniqueID
        if (perSettingsData.containsKey(uniqueID)) {
            project.thisLogger().warn("[com.github.rebel000.cmdlineargs] $uniqueID already registered")
            return
        }
        val adapter = createAdapter(s, false)?.apply { 
            enabled = projectStorage.enabledConfigs.contains(uniqueID)
            setTrustedByName(projectStorage.trustedConfigs.contains(uniqueID))
        }
        val node = ConfigurationNode(adapter, s).apply {
            paused = !_isEnabled
            visible = !projectStorage.hiddenConfigs.contains(s.uniqueID)
        }
        perSettingsData[uniqueID] = SettingsData(adapter, node)
        invalidate(arguments = true, preview = true)
    }

    internal fun onRunConfigurationChanged(s: RunnerAndConfigurationSettings, existingId: String?) {
        val existingId = existingId ?: return
        val uniqueID = s.uniqueID
        ApplicationManager.getApplication().invokeLater {
            var invalidatePreview = false
            if (uniqueID != existingId) {
                perSettingsData.remove(existingId)?.let {
                    model.rawRemove(it.node)
                    perSettingsData[uniqueID] = it
                    it.adapter?.invalidate(s)
                    it.node.settingsID = s.uniqueID
                    it.node.text = s.getQualifiedDisplayName()
                    it.adapter?.let { adapter -> 
                        it.node.value = adapter.getArguments()
                    }
                }
                invalidatePreview = true
            }
            invalidate(arguments = true, preview = invalidatePreview)
        }
    }

    internal fun onRunConfigurationRemoved(s: RunnerAndConfigurationSettings) {
        val uniqueID = s.uniqueID
        ApplicationManager.getApplication().invokeLater {
            perSettingsData.remove(uniqueID)?.let {
                model.rawRemove(it.node)
                invalidate(preview = true)
            }
        }
    }

    internal fun onRunConfigurationSelected(s: RunnerAndConfigurationSettings?) {
        RunManager.getInstanceIfCreated(project)?.let {
            val activeConfigurations = s?.getEffectiveConfigurations().orEmpty()
            ApplicationManager.getApplication().invokeLater {
                perSettingsData.forEach { (uniqueID, it) ->
                    it.node.active = activeConfigurations.contains(uniqueID)
                    model.invalidate(it.node, false)
                }
            }
        }
    }

    @Suppress("unused")
    internal fun onRunManagerLoaded(runManager: RunManager) {
        _isRunManagerLoaded = true
        invalidate(arguments = true, preview = true)
    }

    internal fun reload() {
        val path = locateStateFile().also { stateFilePath = it }
        val reader = if (path != null) {
            File(path).takeIf { it.exists() }
                ?.let { JsonObjectReader.tryParse(it.readText(), project.thisLogger()) }
        } else {
            XmlObjectReader(projectStorage.projectArguments)
        }
        reader?.let { reader ->
            val filters = getFilters().map { it.key }
            val revision = reader["revision"].asInt ?: reader["version"].asInt ?: 0
            if (revision >= 3) {
                isEnabled = reader["enabled"].asBoolean ?: true
                model.previewRoot.isExpanded = reader["preview"].asBoolean ?: true
                reader["root"].asObject?.let { root ->
                    model.projectRoot.deserialize(root, revision) { node ->
                        if (node is ArgumentNode) {
                            node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                        }
                    }
                }
            } else if (revision > 0) {
                isEnabled = reader["isEnabled"].asBoolean ?: true
                model.previewRoot.isExpanded = true
                model.projectRoot.deserialize(reader, revision) { node ->
                    if (node is ArgumentNode) {
                        node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                    }
                }
            }
        }
        if (globalStorage.showSharedNode) {
            reloadShared()
        }
        model.invalidate()
    }

    private fun createAdapter(s: RunnerAndConfigurationSettings, isRunningCurrentFile: Boolean): ArgumentsAdapter? {
        return ArgumentsAdapterProviderExtension.EP_NAME.extensionList.firstNotNullOfOrNull {
            it.createAdapter(s, isRunningCurrentFile)
        }
    }

    private fun invalidate(arguments: Boolean = false, preview: Boolean = false) {
        if (!_isRunManagerLoaded) {
            return
        }
        if (arguments && _isArgumentsInvalid.compareAndSet(false, true)) {
            ApplicationManager.getApplication().invokeLater {
                _isArgumentsInvalid.set(false)
                rebuildArguments()
            }
        }
        if (preview) {
            _revision++
            if (_isPreviewInvalid.compareAndSet(false, true)) {
                ApplicationManager.getApplication().invokeLater {
                    _isPreviewInvalid.set(false)
                    rebuildPreview()
                }
            }
        }
    }

    private fun locateStateFile(): String? {
        val basePath = project.basePath ?: return null
        if (PlatformExtension.EP_NAME.extensions.any { it.isRider() }) {
            val riderConfig = Path(basePath).resolve(project.name + ".cmdlineargs.json")
            Path(basePath).resolve(project.name + ".ddargs.json").let { oldConfig ->
                if (!riderConfig.exists() && oldConfig.exists()) {
                    oldConfig.copy(riderConfig)
                }
            }
            return riderConfig.toString()
        }
        val workspaceDir = project.workspaceFile?.parent ?: project.projectFile?.parent
        val rootConfig = Path(basePath).resolve(".cmdlineargs.json")
        if (rootConfig.exists() || workspaceDir == null) {
            return rootConfig.toString()
        }
        return null
    }

    private fun onEnabledChanged() {
        perSettingsData.forEach { (_, it) ->
            it.node.paused = !_isEnabled
            model.invalidate(it.node, false)
        }
        invalidate(arguments = true)
        requestSave()
    }

    private fun onShowSharedChanged() {
        if (_isSharedEnabled) {
            reloadShared()
        } else {
            saveShared()
            model.sharedRoot = null
        }
    }

    private fun reloadShared() {
        val reader = if (revision >= 3) {
            XmlObjectReader(globalStorage.sharedArguments)
        } else {
            globalStorage.sharedArgs
                ?.let { Base64.getDecoder().decode(it).toString(Charsets.UTF_8) }
                ?.let { JsonObjectReader.tryParse(it, project.thisLogger()) }
        }
        reader?.let { reader ->
            ArgumentContainer(Messages.message("toolwindow.sharedNode")).also {  node ->
                model.sharedRoot = node
                val filters = getFilters().map { it.key }
                node.deserialize(reader, revision) { node ->
                    if (node is ArgumentNode) {
                        node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                    }
                }.ifFalse { project.thisLogger().warn("[com.github.rebel000.cmdlineargs] Failed to deserialize shared arguments") }
            }
        }
    }

    private fun save() {
        val writer = stateFilePath?.let { JsonObjectWriter() } ?: XmlObjectWriter("state")
        writer["revision"] = SERIALIZE_REVISION
        writer["enabled"] = isEnabled
        writer["preview"] = model.previewRoot.isExpanded
        model.projectRoot.serialize(writer.addObject("root"))
        projectStorage.enabledConfigs = model.previewRoot.children()
            .asSequence()
            .mapNotNull { node ->
                (node as? ConfigurationNode)?.takeIf { it.isChecked }?.settingsID
            }.toMutableSet()
        when (writer) {
            is JsonObjectWriter -> {
                File(stateFilePath!!).writeText(writer.toString())
            }

            is XmlObjectWriter -> {
                projectStorage.projectArguments = writer.xElement
            }
        }
        saveShared()
        projectStorage.trustedConfigs = perSettingsData
            .asSequence()
            .mapNotNull{ (k, v) -> k.takeIf { v.adapter?.isTrustedByName() == true }}
            .toMutableSet()
        projectStorage.hiddenConfigs = perSettingsData
            .asSequence()
            .mapNotNull{ (k, v) -> k.takeIf { !v.node.visible }}
            .toMutableSet()
    }

    private fun saveShared() {
        model.sharedRoot?.let { root ->
            with(globalStorage) {
                revision = SERIALIZE_REVISION
                sharedArguments = XmlObjectWriter("args").also { w -> root.serialize(w) }.xElement
                sharedArgs = null
            }
        }
    }

    private fun requestSave() {
        _saveJob?.cancel()
        _saveJob = cs.launch {
            delay(DEFERRED_SAVE_DELAY)
            withContext(Dispatchers.EDT){
                save()
            }
        }
    }

    private fun rebuildArguments() {
        if (isEnabled) {
            val visitors = mutableListOf<Pair<ArgumentsAdapter?, CollectArgsVisitor>>()
            perSettingsData.forEach { (_, it) ->
                if (it.node.visible && it.node.isChecked && it.adapter?.isTrusted() == true) {
                    visitors.add(it.adapter to CollectArgsVisitor(it.adapter.predicate()))
                }
            }
            visitors.add(null to CollectArgsVisitor { it.filters.all { (_, f) -> f.isEmpty() } })
            val multiVisitor = object : TraverseVisitor<ArgumentNode> {
                private val skip = IntArray(visitors.size) { 0 }
                override fun onEnter(node: ArgumentNode): Boolean {
                    var shouldEnter = false
                    for (i in visitors.indices) {
                        if (skip[i] == 0 && visitors[i].second.onEnter(node)) {
                            shouldEnter = true
                        } else {
                            skip[i]++
                        }
                    }
                    if (!shouldEnter) {
                        for (i in skip.indices) {
                            if (skip[i] > 0) {
                                skip[i]--
                            }
                        }
                    }
                    return shouldEnter
                }
                override fun onExit(node: ArgumentNode) {
                    for (i in visitors.indices) {
                        if (skip[i] > 0) {
                            skip[i]--
                        } else {
                            visitors[i].second.onExit(node)
                        }
                    }
                }
            }
            model.sharedRoot?.traverse(multiVisitor)
            model.projectRoot.traverse(multiVisitor)
            for (it in visitors) {
                val (adapter, visitor) = it
                if (adapter != null) {
                    val value = visitor.toString()
                    adapter.setArguments(value)
                    perSettingsData[adapter.key]?.let { (_, node) ->
                        node.value = value
                        model.invalidate(node, false)
                    }
                } else {
                    _commonArguments = visitor.toString()
                }
            }
        }
    }

    private fun rebuildPreview() {
        val allConfigs = perSettingsData.toList().sortedBy { it.first }
        val activeConfigurations =
            RunManager.getInstanceIfCreated(project)?.selectedConfiguration?.getEffectiveConfigurations()
        var after: ArgumentTreeNodeBase? = null
        var visibleNodes = 0
        for ((uniqueID, it) in allConfigs) {
            val (adapter, node) = it
            activeConfigurations?.let { node.active = it.contains(uniqueID) }
            val visible = node.visible || showHidden
            val trusted = adapter?.isTrusted() == true
            val supported = adapter != null
            if (visible && (trusted || (supported && showExperimental) || (!supported && showNotSupported))) {
                if (node.parent == null) {
                    val index = if (after != null) {
                        model.previewRoot.getIndex(after) + 1
                    } else {
                        0
                    }
                    model.rawInsert(node, model.previewRoot, index)
                }
                after = node
                visibleNodes++
            } else {
                if (node.parent != null) {
                    model.rawRemove(node)
                }
            }
        }
        if (visibleNodes != model.previewRoot.childCount) {
            // this should never happen
            model.previewRoot.children()
                .asSequence()
                .mapNotNull {
                    it.takeIf {
                        it !is ConfigurationNode || !perSettingsData.containsKey(it.settingsID)
                    } as? ArgumentTreeNodeBase
                }.forEach {
                    model.rawRemove(it)
                }
        }
        model.invalidate(model.previewRoot, true)
    }

    private fun onArgumentsChanged(nodes: List<ArgumentTreeNodeBase>) {
        var shouldInvalidate = false
        var shouldSave = false
        for (node in nodes) {
            when (node) {
                is ArgumentContainer -> {
                    shouldInvalidate = true
                    shouldSave = true
                }

                is ConfigurationNode -> node.settingsID.let {
                    perSettingsData[it]?.adapter?.let { adapter ->
                        val enabled = node.visible && node.isChecked
                        if (adapter.enabled != enabled) {
                            adapter.enabled = enabled
                            if (isEnabled && enabled && adapter.isTrusted()) {
                                val visitor = CollectArgsVisitor(adapter.predicate())
                                model.sharedRoot?.traverse(visitor)
                                model.projectRoot.traverse(visitor)
                                val value = visitor.toString()
                                adapter.setArguments(value)
                                node.value = value
                            }
                            shouldSave = true
                        }
                    }
                }
            }
        }
        if (shouldInvalidate) {
            invalidate(arguments = true)
        }
        if (shouldSave) {
            requestSave()
        }
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        e?.children
            ?.filterIsInstance<ArgumentTreeNodeBase>()
            ?.ifNotEmpty { onArgumentsChanged(this) }
    }

    override fun treeNodesInserted(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            onArgumentsChanged(listOf(it))
        }
    }

    override fun treeNodesRemoved(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            onArgumentsChanged(listOf(it))
        }
    }

    override fun treeStructureChanged(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            onArgumentsChanged(listOf(it))
        }
    }
}

