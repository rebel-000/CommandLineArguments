package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.actions.CopyArgsAction
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.github.rebel000.cmdlineargs.extensions.PlatformExtension
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.tree.*
import com.github.rebel000.cmdlineargs.tree.visitors.CollectArgsVisitor
import com.github.rebel000.cmdlineargs.tree.visitors.TraverseVisitor
import com.github.rebel000.cmdlineargs.helpers.asBooleanOrNull
import com.github.rebel000.cmdlineargs.helpers.asIntOrNull
import com.github.rebel000.cmdlineargs.helpers.asJsonArrayOrNull
import com.github.rebel000.cmdlineargs.helpers.asJsonObjectOrNull
import com.github.rebel000.cmdlineargs.helpers.asStringOrNull
import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterKey
import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterName
import com.github.rebel000.cmdlineargs.helpers.tryParseJson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.multilaunch.MultiLaunchConfiguration
import com.intellij.execution.multilaunch.execution.executables.impl.RunConfigurationExecutableManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Service(Service.Level.PROJECT)
class ArgumentsService(val project: Project, coroScope: CoroutineScope) : Disposable {
    companion object {
        const val SERIALIZE_REVISION: Int = 3
        val DEFERRED_SAVE_DELAY: Duration = 500.milliseconds

        fun getInstance(project: Project): ArgumentsService = project.service()
        fun getInstanceIfCreated(project: Project?): ArgumentsService? = project?.serviceIfCreated()
    }

    private val adapters = mutableMapOf<Pair<String, String>, ArgumentsAdapter>()
    private val saveFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val disabledConfigurations = mutableSetOf<Pair<String, String>>()
    private var stateFile: String? = null
    private var _isEnabled = true
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

    init {
        model.addTreeModelListener(object : TreeModelListener {
            override fun treeNodesChanged(e: TreeModelEvent?) {
                val children = e?.children
                if (children != null && children.isNotEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        val nodes = children.filterIsInstance<ArgumentTreeNodeBase>()
                        onArgumentsChanged(nodes)
                    }
                }
            }

            override fun treeNodesInserted(e: TreeModelEvent?) {
                val node = e?.path?.lastOrNull() as? ArgumentTreeNodeBase
                if (node != null) {
                    ApplicationManager.getApplication().invokeLater {
                        onArgumentsChanged(listOf(node))
                    }
                }
            }

            override fun treeNodesRemoved(e: TreeModelEvent?) {
                val node = e?.path?.lastOrNull() as? ArgumentTreeNodeBase
                if (node != null) {
                    ApplicationManager.getApplication().invokeLater {
                        onArgumentsChanged(listOf(node))
                    }
                }
            }

            override fun treeStructureChanged(e: TreeModelEvent?) {
                val node = e?.path?.lastOrNull() as? ArgumentTreeNodeBase
                if (node != null) {
                    ApplicationManager.getApplication().invokeLater {
                        onArgumentsChanged(listOf(node))
                    }
                }
            }
        })
        reload()
        coroScope.launch {
            saveFlow.debounce(DEFERRED_SAVE_DELAY).collectLatest { save() }
        }
        ApplicationManager.getApplication().invokeLater {
            update()
        }
    }

    override fun dispose() {
        save()
    }

    fun isSupported(s: RunnerAndConfigurationSettings?): Boolean {
        return s == null
                || ArgumentsAdapterProviderExtension.EP_NAME.extensionList.any { it.isSupported(s) }
                || s.configuration is CommonProgramRunConfigurationParameters
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
            RunManager.getInstanceIfCreated(project)?.allConfigurationsList.orEmpty().map { it.name }
        ))
    }

    internal fun toggleShared(enabled: Boolean) {
        if (isSharedVisible != enabled) {
            val sharedState = getSharedState()
            sharedState.showSharedNode = enabled
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
            } else if (node is InfoNode) {
                val key = node.userdata
                if (key is Pair<*, *> && key.first is String && key.second is String) {
                    val key = Pair(key.first as String, key.second as String)
                    val adapter = adapters[key]
                    if (adapter != null) {
                        adapter.isEnabled = node.isChecked
                        if (node.isChecked) {
                            val visitor = CollectArgsVisitor(adapter.predicate())
                            model.sharedRoot?.traverse(visitor)
                            model.projectRoot.traverse(visitor)
                            adapter.setArguments(visitor.toString())
                            disabledConfigurations.remove(key)
                        } else {
                            disabledConfigurations.add(key)
                        }
                        updatePreview()
                        save()
                    }
                }
            }
        }
        if (shouldUpdate) {
            saveFlow.tryEmit(Unit)
            update()
        }
    }

    internal fun onProcessStarting(s: RunnerAndConfigurationSettings?) {
        if (isEnabled && s != null) {
            getAdapter(s)?.onStart()
        }
    }

    internal fun onProcessStarted(s: RunnerAndConfigurationSettings?) {
        if (isEnabled && s != null) {
            getAdapter(s)?.onCleanup()
        }
    }

    internal fun onProcessNotStarted(s: RunnerAndConfigurationSettings?) {
        if (isEnabled && s != null) {
            getAdapter(s)?.onCleanup()
        }
    }

    internal fun onRunConfigurationAdded(s: RunnerAndConfigurationSettings) {
        val key = Pair(s.type.id, s.name)
        if (adapters.containsKey(key)) {
            project.thisLogger().warn("[com.github.rebel000.cmdlineargs] Adapter for $key already exists")
            return
        }
        val adapter = createAdapter(s)
        if (adapter != null) {
            adapter.isEnabled = !disabledConfigurations.contains(key)
            adapters[key] = adapter
        }
        update()
        updateCopyActions()
    }

    internal fun onRunConfigurationChanged(s: RunnerAndConfigurationSettings, existingId: String?) {
        val oldKey = existingId?.let { Pair(s.type.id, it) }
        val newKey = Pair(s.type.id, s.name)
        if (oldKey != null && newKey != oldKey) {
            var adapter = getAdapter(s)
            if (adapter != null) {
                adapters.remove(oldKey)
                disabledConfigurations.remove(oldKey)
            } else {
                adapter = createAdapter(s)
            }
            if (adapter != null) {
                if (!adapter.isEnabled) {
                    disabledConfigurations.add(newKey)
                }
                adapters[newKey] = adapter
            }
            updateCopyActions()
        }
        update()
    }

    internal fun onRunConfigurationRemoved(s: RunnerAndConfigurationSettings) {
        val key = Pair(s.type.id, s.name)
        adapters.remove(key)
        disabledConfigurations.remove(key)
        update()
        updateCopyActions()
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
        if (s.configuration is CommonProgramRunConfigurationParameters) {
            return object : ArgumentsAdapter(s) {
                override fun getArguments(): String {
                    return (settings.configuration as? CommonProgramRunConfigurationParameters)?.programParameters ?: ""
                }
                override fun setArguments(value: String) {
                    (settings.configuration as? CommonProgramRunConfigurationParameters)?.programParameters = value.nullIfEmpty()
                }
            }
        }
        return null
    }

    private fun getAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        val key = Pair(s.type.id, s.name)
        return adapters[key]
    }

    private fun getSharedState(): ArgumentsSharedStorage.State {
        return ApplicationManager.getApplication().getService(ArgumentsSharedStorage::class.java).state
    }

    internal fun reload() {
        val stateFile = File(stateFile ?: locateStateFile())
        this.stateFile = stateFile.path
        if (stateFile.exists()) {
            val jsonString = stateFile.readText()
            val jObject: JsonObject? = tryParseJson(jsonString, project.thisLogger())?.asJsonObjectOrNull
            if (jObject != null) {
                val revision = (jObject.get("revision") ?: jObject.get("version"))?.asIntOrNull ?: 0
                if (revision >= 3) {
                    isEnabled = jObject.get("enabled")?.asBooleanOrNull ?: true
                    model.previewRoot.isExpanded = jObject.get("preview")?.asBooleanOrNull ?: true
                    val jDisabled = jObject.get("disabled")?.asJsonArrayOrNull
                    if (jDisabled != null) {
                        for (it in jDisabled) {
                            if (it is JsonArray && it.size() == 2) {
                                val type = it[0].asStringOrNull
                                val name = it[1].asStringOrNull
                                if (type != null && name != null) {
                                    disabledConfigurations.add(Pair(type, name))
                                }
                            }
                        }
                    }
                    val filters = getFilters().map { it.key }
                    val jRoot = jObject.get("root")?.asJsonObjectOrNull
                    if (jRoot != null) {
                        model.projectRoot.deserialize(jRoot, revision) { node ->
                            if (node is ArgumentNode) {
                                node.filters = node.filters.filter { it.key in filters }
                            }
                        }
                    }
                } else {
                    val filters = getFilters().map { it.key }
                    isEnabled = jObject.get("isEnabled")?.asBooleanOrNull ?: true
                    model.previewRoot.isExpanded = true
                    model.projectRoot.deserialize(jObject, 1) { node ->
                        if (node is ArgumentNode) {
                            node.filters = node.filters.filter { it.key in filters }
                        }
                    }
                }
            }
        }
        if (isSharedVisible) {
            reloadShared()
        }
        model.invalidate()
    }

    private fun reloadShared() {
        val sharedState = getSharedState()
        val revision = sharedState.revision
        var jsonString = sharedState.sharedArgs ?: return
        if (revision == SERIALIZE_REVISION) {
            jsonString = Base64.getDecoder().decode(jsonString).toString(Charsets.UTF_8)
        }
        if (jsonString.isNotEmpty()) {
            val jObject: JsonObject? = tryParseJson(jsonString, project.thisLogger())?.asJsonObjectOrNull
            if (jObject != null) {
                val node = ArgumentContainer(Messages.message("toolwindow.sharedNode"))
                val filters = getFilters().map { it.key }
                val isValid = node.deserialize(jObject, revision) { node ->
                    if (node is ArgumentNode) {
                        node.filters = node.filters.filter { it.key in filters }
                    }
                }
                if (isValid) {
                    model.sharedRoot = node
                }
            }
        }
    }

    private fun save() {
        val stateFile = File(stateFile ?: locateStateFile())
        this.stateFile = stateFile.path
        val jObject = JsonObject()
        jObject.addProperty("revision", SERIALIZE_REVISION)
        jObject.addProperty("enabled", isEnabled)
        jObject.addProperty("preview", model.previewRoot.isExpanded)
        jObject.add("disabled", JsonArray().apply {
            for (node in model.previewRoot.children()) {
                if (node is InfoNode && !node.isChecked) {
                    val key = node.userdata
                    if (key is Pair<*, *> && key.first is String && key.second is String) {
                        add(JsonArray().apply {
                            add(key.first as String)
                            add(key.second as String)
                        })
                    }
                }
            }
        })
        jObject.add("root", model.projectRoot.serialize())
        stateFile.writeText(jObject.toString())
        saveShared()
    }

    private fun saveShared() {
        val sharedNode = model.sharedRoot
        if (sharedNode != null) {
            val sharedState = getSharedState()
            val jsonString = sharedNode.serialize().toString()
            sharedState.revision = SERIALIZE_REVISION
            sharedState.sharedArgs = Base64.getEncoder().encodeToString(jsonString.toByteArray(Charsets.UTF_8))
        }
    }

    private fun locateStateFile(): String {
        val projectDir = File(project.basePath!!)
        val projectSettingsFile = File(projectDir, project.name + ".ddargs.json")
        if (!projectSettingsFile.exists() && !PlatformExtension.EP_NAME.extensions.any { it.isRider() }) {
            val ideaDir = projectDir.resolve(Project.DIRECTORY_STORE_FOLDER)
            if (ideaDir.isDirectory()) {
                return ideaDir.resolve(project.name + ".ddargs.json").path
            }
        }
        return projectSettingsFile.path
    }

    private fun update() {
        if (isEnabled) {
            val visitors = mutableListOf<CollectArgsVisitor>()
            val collectArgsVisitorMap = mutableMapOf<Pair<String, String>, CollectArgsVisitor>()
            for ((key, value) in adapters) {
                if (value.isEnabled) {
                    val visitor = CollectArgsVisitor(value.predicate())
                    visitors.add(visitor)
                    collectArgsVisitorMap[key] = visitor
                }
            }
            val multiVisitor = object : TraverseVisitor {
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
                if (value.isEnabled) {
                    val arguments = collectArgsVisitorMap[key]?.toString() ?: ""
                    value.setArguments(arguments)
                }
            }
        }
        ApplicationManager.getApplication().invokeLater {
            updatePreview()
        }
    }

    private fun updateCopyActions() {
        val action = ActionManager.getInstance().getAction("cmdlineargs.copy-args") as? CopyArgsAction ?: return
        val allSettings = RunManager.getInstanceIfCreated(project)?.allSettings.orEmpty()
        action.removeAll()
        for (it in allSettings) {
            if (isSupported(it)) {
                action.add(CopyArgsAction.CopyArgsForSettings(it))
            }
        }
    }

    private fun updatePreview() {
        val runManager = RunManager.getInstanceIfCreated(project) ?: return
        val allConfigurations = runManager.allSettings.filter { it.configuration !is CompoundRunConfiguration && it.configuration !is MultiLaunchConfiguration }
        val activeConfigurations = when (val cfg = runManager.selectedConfiguration?.configuration) {
            is CompoundRunConfiguration -> cfg.getConfigurationsWithEffectiveRunTargets().map { it.configuration }
            is MultiLaunchConfiguration -> { cfg.descriptors.mapNotNull { (it.executable as? RunConfigurationExecutableManager.RunConfigurationExecutable)?.settings?.configuration }}
            else -> listOf(cfg)
        }
        if (model.previewRoot.childCount != allConfigurations.size) {
            while (model.previewRoot.childCount < allConfigurations.size) {
                model.rawInsert(InfoNode(""), model.previewRoot, model.previewRoot.childCount)
            }
            while (model.previewRoot.childCount > allConfigurations.size) {
                model.remove(model.previewRoot.lastChild as ArgumentTreeNodeBase)
            }
        }
        for (i in 0 until allConfigurations.size) {
            val config = allConfigurations[i]
            val adapter = getAdapter(config)
            val node = model.previewRoot.getChildAt(i) as InfoNode
            val isActive = config.configuration in activeConfigurations
            val isSupported = adapter != null
            node.isEnabled = isSupported
            if (isSupported) {
                node.isChecked = adapter.isEnabled
                node.icon = when {
                    !isEnabled || !adapter.isEnabled -> AllIcons.Actions.Pause
                    isActive -> AllIcons.Actions.Execute
                    else -> AllIcons.Toolwindows.ToolWindowRun
                }
                node.style = when {
                    isActive -> InfoNode.SUCCESS_TEXT_ATTRIBUTES
                    else -> null
                }
                node.text = "[${config.type.displayName}] ${config.name}: ${adapter.getArguments()}"
                node.userdata = Pair(config.type.id, config.name)
            } else {
                node.icon = AllIcons.Run.ShowIgnored
                node.style = when {
                    isActive -> InfoNode.WARN_TEXT_ATTRIBUTES
                    else -> null
                }
                node.text = "[${config.type.displayName}] ${config.name}: ${Messages.message("toolwindow.notSupportedNode")}"
                node.userdata = null
            }
        }
        model.invalidate(model.previewRoot, true)
    }
}

