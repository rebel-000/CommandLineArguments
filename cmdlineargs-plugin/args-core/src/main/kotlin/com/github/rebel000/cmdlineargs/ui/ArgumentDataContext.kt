package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.tree.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.util.Disposer
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener

internal class ArgumentDataContext(val service: ArgumentsService, val tree: ArgumentTree) : TreeModelListener, TreeSelectionListener, Disposable {
    companion object {
        val KEY = DataKey.create<ArgumentDataContext>("CMDLINEARGS_DATA_CONTEXT")
    }

    private var _disposed = false

    val disposed: Boolean get() = _disposed
    val model: ArgumentTreeModel get() = service.model

    @Volatile
    var treeIsEditing = false

    @Volatile
    var treeIsTrustedByName = false
    @Volatile
    var treeIsTrustedByType = false

    @Volatile
    var treeSelectedConfigurations: Int = 0
    @Volatile
    var treeSelectedExperimental: Int = 0
    @Volatile
    var treeSelectedContainers: Int = 0
    @Volatile
    var treeSelectedArguments: Int = 0
    @Volatile
    var treeSelectedCount: Int = 0

    fun install(disposable: Disposable) {
        tree.addTreeSelectionListener(this)
        service.model.addTreeModelListener(this)
        Disposer.register(disposable, this)
    }
    
    override fun dispose() {
        _disposed = true
        tree.removeTreeSelectionListener(this)
        service.model.removeTreeModelListener(this)
    }

    override fun treeNodesChanged(e: TreeModelEvent?) {
        if (treeSelectedExperimental == 0) {
            treeIsTrustedByName = false
            treeIsTrustedByType = false
            return
        }
        for (path in tree.selectionPaths.orEmpty()) {
            (path.lastPathComponent as? ConfigurationNode)
                ?.settingsID
                ?.let { service.findAdapter(it) }
                ?.let { adapter ->
                    treeIsTrustedByName = treeIsTrustedByName || adapter.isTrustedByName() == true
                    treeIsTrustedByType = treeIsTrustedByType || adapter.isTrustedByType() == true
                    if (treeIsTrustedByName && treeIsTrustedByType) break
                }
        }
    }
    override fun treeNodesInserted(e: TreeModelEvent?)  = Unit
    override fun treeNodesRemoved(e: TreeModelEvent?)  = Unit
    override fun treeStructureChanged(e: TreeModelEvent?)  = Unit

    override fun valueChanged(e: TreeSelectionEvent?) {
        treeIsTrustedByName = false
        treeIsTrustedByType = false
        treeSelectedArguments = 0
        treeSelectedContainers = 0
        treeSelectedConfigurations = 0
        treeSelectedExperimental = 0
        treeSelectedCount = tree.selectionCount
        for (path in tree.selectionPaths.orEmpty()) {
            when (val node = path.lastPathComponent) {
                is ArgumentNode -> {
                    treeSelectedArguments++
                    treeSelectedContainers++
                }

                is ArgumentContainer -> {
                    treeSelectedContainers++
                }

                is ConfigurationNode -> {
                    treeSelectedConfigurations++
                    if (node.isExperimental) {
                        treeSelectedExperimental++
                        if (!treeIsTrustedByName || !treeIsTrustedByType) {
                            node.settingsID
                                ?.let { service.findAdapter(it) }
                                ?.let {
                                    treeIsTrustedByName = treeIsTrustedByName || (it.isTrustedByName() == true)
                                    treeIsTrustedByType = treeIsTrustedByType || (it.isTrustedByType() == true)
                                }
                        }
                    }
                }
            }
        }
    }
}