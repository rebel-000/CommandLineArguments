package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.actions.CopyCommandLineActionGroup
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.github.rebel000.cmdlineargs.extensions.PlatformExtension
import com.github.rebel000.cmdlineargs.helpers.*
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
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
import com.intellij.execution.multilaunch.execution.executables.impl.RunConfigurationExecutableManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.util.io.copy
import com.intellij.util.io.move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import java.util.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
class ArgumentsService(val project: Project, coroScope: CoroutineScope) : Disposable, TreeModelListener {
    companion object {
        const val SERIALIZE_REVISION: Int = 3
        val DEFERRED_SAVE_DELAY: Duration = 1.seconds

        fun getInstance(project: Project): ArgumentsService = project.service()
        fun getInstanceIfCreated(project: Project?): ArgumentsService? = project?.serviceIfCreated()
    }

    private var _revision = -1

    private val adapters = mutableMapOf<String, ArgumentsAdapter>()
    private val saveFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var stateFilePath: String? = locateStateFile()
    private var _isEnabled = true
    internal var copyArgsActions: Array<AnAction> = AnAction.EMPTY_ARRAY
    internal val model = ArgumentTreeModel()
    internal val isSharedVisible get() = model.sharedRoot != null
    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            if (_isEnabled != value) {
                _isEnabled = value
                saveFlow.tryEmit(Unit)
                update()
            }
        }

    var showSharedArguments
        get() = model.sharedRoot != null
        set(value) {
            val isSharedEnabled = model.sharedRoot != null
            if (isSharedEnabled != value) {
                globalStorage.showSharedNode = value
                if (value) {
                    reloadShared()
                } else {
                    saveShared()
                    model.sharedRoot = null
                }
            }
        }

    var showExperimental
        get() = getGlobalStorage().showExperimental
        set(value) {
            getGlobalStorage().showExperimental = value
            updatePreview()
        }

    var showUnsupported
        get() = getGlobalStorage().showUnsupported
        set(value) {
            getGlobalStorage().showUnsupported = value
            updatePreview()
        }

    val revision: Int get() = _revision

    init {
        model.addTreeModelListener(this)
        coroScope.launch { saveFlow.debounce(DEFERRED_SAVE_DELAY).collectLatest { save() } }
        ApplicationManager.getApplication().invokeLater {
            update()
            updateCopyActions()
        }
    }

    override fun dispose() {
        save()
    }

    fun isSupported(s: RunnerAndConfigurationSettings?): Boolean {
        return s == null
            || ArgumentsAdapterProviderExtension.EP_NAME.extensionList.any { it.isSupported(s) }
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
                        it.takeIf { getAdapter(it)?.isTrusted() == true }?.getArgumentsAdapterFilterKey()
                    }.distinct()
            )
        )
    }

    internal fun toggleShared(enabled: Boolean) {
        if (isSharedVisible != enabled) {
            val globalState = getGlobalStorage()
            globalState.showSharedNode = enabled
            if (enabled) {
                reloadShared()
            } else {
                saveShared()
                model.sharedRoot = null
            }
        }
    }

    private fun onArgumentsChanged(nodes: List<ArgumentTreeNodeBase>) {
        var shouldUpdate = false
        for (node in nodes) {
            if (node is ArgumentContainer) {
                shouldUpdate = true
            } else if (node is ConfigurationNode) {
                val adapter = adapters[node.key ?: continue]
                if (adapter != null) {
                    adapter.enabled = node.isChecked
                    if (node.isChecked) {
                        val visitor = CollectArgsVisitor(adapter.predicate())
                        model.sharedRoot?.traverse(visitor)
                        model.projectRoot.traverse(visitor)
                        adapter.setArguments(visitor.toString())
                    }
                    updatePreview()
                    saveFlow.tryEmit(Unit)
                }
            }
        }
        if (shouldUpdate) {
            saveFlow.tryEmit(Unit)
            update()
        }
    }

    internal fun markDirty() {
        _revision++
    }

    internal fun onProcessStarting(s: RunnerAndConfigurationSettings?) {
        if (!isEnabled || s == null) {
            return
        }

        val adapter = getAdapter(s)
        if (adapter != null) {
            adapter.onStart()
        } else {
            val tempAdapter = createAdapter(s)
            if (tempAdapter?.isTrusted() == true) {
                val visitor = CollectArgsVisitor(tempAdapter.predicate())
                model.sharedRoot?.traverse(visitor)
                model.projectRoot.traverse(visitor)
                tempAdapter.setArguments(visitor.toString())
            }
        }
    }

    internal fun onProcessStarted(s: RunnerAndConfigurationSettings?) {
        if (!isEnabled || s == null) {
            return
        }

        getAdapter(s)?.onCleanup()
    }

    internal fun onProcessNotStarted(s: RunnerAndConfigurationSettings?) {
        if (!isEnabled || s == null) {
            return
        }

        getAdapter(s)?.onCleanup()
    }

    internal fun onRunConfigurationAdded(s: RunnerAndConfigurationSettings) {
        val key = s.getArgumentsAdapterKey()
        if (adapters.containsKey(key)) {
            project.thisLogger().warn("[com.github.rebel000.cmdlineargs] Adapter for $key already exists")
            return
        }
        val adapter = createAdapter(s)
        if (adapter != null) {
            adapter.enabled = getProjectStorage().enabledConfigs.contains(key)
            adapters[key] = adapter
            markDirty()
        }
        update()
    }

    internal fun onRunConfigurationChanged(s: RunnerAndConfigurationSettings, existingId: String?) {
        val oldKey = existingId?.let { "${s.type.id}:${it}" }
        val newKey = s.getArgumentsAdapterKey()
        if (oldKey != null && newKey != oldKey) {
            var adapter = getAdapter(s)
            if (adapter != null) {
                adapters.remove(oldKey)
                getProjectStorage().enabledConfigs.remove(oldKey)
            } else {
                adapter = createAdapter(s)
            }
            if (adapter != null) {
                if (adapter.enabled) {
                    getProjectStorage().enabledConfigs.add(newKey)
                }
                adapters[newKey] = adapter
            }
            markDirty()
        }
        update()
    }

    internal fun onRunConfigurationRemoved(s: RunnerAndConfigurationSettings) {
        markDirty()
        val key = s.getArgumentsAdapterKey()
        adapters.remove(key)
        getProjectStorage().enabledConfigs.remove(key)
        update()
    }

    @Suppress("unused")
    internal fun onRunConfigurationSelected(s: RunnerAndConfigurationSettings?) {
        updatePreview()
    }

    private fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        for (it in ArgumentsAdapterProviderExtension.EP_NAME.extensionList) {
            val adapter = it.createAdapter(s)
            if (adapter != null) {
                return adapter
            }
        }
        return null
    }

    internal fun findAdapter(key: String): ArgumentsAdapter? {
        return adapters[key]
    }

    fun getAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        val adapter = adapters[s.getArgumentsAdapterKey()]
        return adapter?.takeIf { it.settings === s && it.isVisible(showExperimental) }
    }

    private fun getGlobalStorage(): ArgumentsGlobalStorage.State {
        return ArgumentsGlobalStorage.getInstance().state
    }

    internal fun getProjectStorage(): ArgumentsProjectStorage.State {
        return ArgumentsProjectStorage.getInstance(project).state
    }

    internal fun reload() {
        stateFilePath = locateStateFile()
        var serializer: ObjectReader? = null
        if (stateFilePath != null) {
            val stateFile = File(stateFilePath!!)
            if (stateFile.exists()) {
                tryParseJson(stateFile.readText(), project.thisLogger())?.asJsonObjectOrNull?.let { 
                    serializer = JsonObjectReader(it)
                }
            }
        } else {
            serializer = XmlObjectReader(getProjectStorage().args)
        }
        if (serializer != null) {
            val revision = serializer.get("revision").asInt ?: serializer.get("version").asInt ?: 0
            if (revision >= 3) {
                isEnabled = serializer.get("enabled").asBoolean ?: true
                model.previewRoot.isExpanded = serializer.get("preview").asBoolean ?: true
                val filters = getFilters().map { it.key }
                val oRoot = serializer.get("root").asObject
                if (oRoot != null) {
                    model.projectRoot.deserialize(oRoot, revision) { node ->
                        if (node is ArgumentNode) {
                            node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                        }
                    }
                }
            } else if (revision > 0) {
                val filters = getFilters().map { it.key }
                isEnabled = serializer.get("isEnabled").asBoolean ?: true
                model.previewRoot.isExpanded = true
                model.projectRoot.deserialize(serializer, 1) { node ->
                    if (node is ArgumentNode) {
                        node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                    }
                }
            }
        }
        if (showSharedArguments) {
            reloadShared()
        }
        model.invalidate()
    }

    private fun reloadShared() {
        val globalState = getGlobalStorage()
        val revision = globalState.revision
        val serializer = if (revision >= 3) {
            XmlObjectReader(globalState.args)
        } else {
            globalState.sharedArgs?.let {
                val jsonString = Base64.getDecoder().decode(it).toString(Charsets.UTF_8)
                tryParseJson(jsonString, project.thisLogger())?.asJsonObjectOrNull?.let { json ->
                    JsonObjectReader(json)
                }
            }
        }
        if (serializer != null) {
            val node = ArgumentContainer(Messages.message("toolwindow.sharedNode"))
            val filters = getFilters().map { it.key }
            val isValid = node.deserialize(serializer, revision) { node ->
                if (node is ArgumentNode) {
                    node.filters = node.filters.filter { it.key in filters }.toMutableMap()
                }
            }
            if (isValid) {
                model.sharedRoot = node
            }
        }
    }

    private fun save() {
        val serializer = if (stateFilePath != null) {
            JsonObjectWriter()
        }
        else {
            XmlObjectWriter("state")
        }
        serializer.add("revision", SERIALIZE_REVISION)
        serializer.add("enabled", isEnabled)
        serializer.add("preview", model.previewRoot.isExpanded)
        model.projectRoot.serialize(serializer.addObject("root"))
        when (serializer) {
            is JsonObjectWriter -> File(stateFilePath!!).writeText(serializer.toString())
            is XmlObjectWriter -> getProjectStorage().args = serializer.xElement
        }
        getProjectStorage().enabledConfigs = model.previewRoot.children()
            .asSequence()
            .mapNotNull {
                if (it is ConfigurationNode && it.isChecked) it.key else null
            }.toMutableSet()
        saveShared()
    }

    private fun saveShared() {
        val sharedNode = model.sharedRoot
        if (sharedNode != null) {
            val serializer = XmlObjectWriter("args")
            val globalState = getGlobalStorage()
            sharedNode.serialize(serializer)
            globalState.revision = SERIALIZE_REVISION
            globalState.args = serializer.xElement
            globalState.sharedArgs = null
        }
    }

    private fun locateStateFile(): String? {
        if (PlatformExtension.EP_NAME.extensions.any { it.isRider() }) {
            val riderConfig = Path(project.basePath!!).resolve(project.name + ".cmdlineargs.json")
            val oldRiderConfig = Path(project.basePath!!).resolve(project.name + ".ddargs.json")
            if (oldRiderConfig.exists()) {
                oldRiderConfig.copy(riderConfig)
                oldRiderConfig.move(oldRiderConfig.parent.resolve(oldRiderConfig.name + ".json.bak"))
            }
            return riderConfig.toString()
        }
        val projectDir = Path(project.basePath!!)
        val workspaceDir = project.workspaceFile?.parent ?: project.projectFile?.parent
        val rootConfig = projectDir.resolve(".cmdlineargs.json")
        if (rootConfig.exists() || workspaceDir == null) {
            return rootConfig.toString()
        }
        return null
    }

    private fun update() {
        if (isEnabled) {
            val visitors = ArrayList<CollectArgsVisitor>(adapters.size)
            val collectArgsVisitorMap = mutableMapOf<String, CollectArgsVisitor>()
            for ((key, value) in adapters) {
                if (value.enabled) {
                    val visitor = CollectArgsVisitor(value.predicate())
                    visitors.add(visitor)
                    collectArgsVisitorMap[key] = visitor
                }
            }
            val multiVisitor = object : TraverseVisitor<ArgumentNode> {
                private val skip = IntArray(visitors.size) { 0 }
                override fun onEnter(node: ArgumentNode): Boolean {
                    var shouldEnter = false
                    for (i in visitors.indices) {
                        if (skip[i] == 0 && visitors[i].onEnter(node)) {
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
                            visitors[i].onExit(node)
                        }
                    }
                }
            }
            model.sharedRoot?.traverse(multiVisitor)
            model.projectRoot.traverse(multiVisitor)
            for ((key, value) in adapters) {
                if (value.enabled) {
                    val arguments = collectArgsVisitorMap[key]?.toString() ?: ""
                    value.setArguments(arguments)
                }
            }
        }
        ApplicationManager.getApplication().invokeLater {
            updatePreview()
        }
    }

    internal fun updateCopyActions() {
        val allSettings = RunManager.getInstanceIfCreated(project)?.allSettings.orEmpty()
        copyArgsActions = allSettings.mapNotNull {
            if (getAdapter(it)?.isTrusted() == true) {
                CopyCommandLineActionGroup.Action(it)
            } else {
                null
            }
        }.toTypedArray()
    }

    internal fun updatePreview() {
        val runManager = RunManager.getInstanceIfCreated(project) ?: return
        var allSettings = runManager.allSettings.filter { it.configuration !is CompoundRunConfiguration && it.configuration !is MultiLaunchConfiguration }
        val activeConfigurations = when (val cfg = runManager.selectedConfiguration?.configuration) {
            is CompoundRunConfiguration -> cfg.getConfigurationsWithEffectiveRunTargets().map { it.configuration }
            is MultiLaunchConfiguration -> { cfg.descriptors.mapNotNull { (it.executable as? RunConfigurationExecutableManager.RunConfigurationExecutable)?.settings?.configuration }}
            else -> listOf(cfg)
        }
        if (!showExperimental || !showUnsupported) {
            allSettings = allSettings.filter {
                val adapter = getAdapter(it)
                when {
                    adapter == null -> showUnsupported
                    adapter.isExperimental() -> showExperimental || showUnsupported
                    else -> true
                }
            }
        }
        if (model.previewRoot.childCount != allSettings.size) {
            while (model.previewRoot.childCount < allSettings.size) {
                model.rawInsert(ConfigurationNode(""), model.previewRoot, model.previewRoot.childCount)
            }
            while (model.previewRoot.childCount > allSettings.size) {
                model.rawRemove(model.previewRoot.lastChild as ArgumentTreeNodeBase)
            }
        }
        for (i in 0 until allSettings.size) {
            val node = model.previewRoot.getChildAt(i) as ConfigurationNode
            val config = allSettings[i]
            val isActive = config.configuration in activeConfigurations
            val adapter = getAdapter(config)?.takeIf { showExperimental || !it.isExperimental() }
            if (!showUnsupported && adapter == null) continue 
            node.setConfiguration(config, adapter, isActive, isEnabled)
        }
        model.invalidate(model.previewRoot, true)
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        e?.children
            ?.filterIsInstance<ArgumentTreeNodeBase>()
            ?.ifNotEmpty { ApplicationManager.getApplication().invokeLater { onArgumentsChanged(this) } }
    }

    override fun treeNodesInserted(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            ApplicationManager.getApplication().invokeLater { onArgumentsChanged(listOf(it)) }
        }
    }

    override fun treeNodesRemoved(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            ApplicationManager.getApplication().invokeLater { onArgumentsChanged(listOf(it)) }
        }
    }

    override fun treeStructureChanged(e: TreeModelEvent?) {
        (e?.treePath?.lastPathComponent as? ArgumentTreeNodeBase)?.let {
            ApplicationManager.getApplication().invokeLater { onArgumentsChanged(listOf(it)) }
        }
    }
}

