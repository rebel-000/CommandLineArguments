package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.actions.CopyPasteProvider
import com.github.rebel000.cmdlineargs.actions.RenameNextAction
import com.github.rebel000.cmdlineargs.actions.RenamePrevAction
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeDnDSupport
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener

internal class ArgumentToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true), TreeModelListener, Disposable {
    private val context: ArgumentDataContext
    private val copyPasteProvider = CopyPasteProvider()
    private val tree: ArgumentTree

    init {
        val service = ArgumentsService.getInstance(project)
        tree = ArgumentTree(service.model)
        ArgumentTreeDnDSupport(tree).install(this)
        ArgumentDataContext(service, tree).also { context = it }.install(this)
        installActions()
        service.model.addTreeModelListener(this)
        add(ScrollPaneFactory.createScrollPane(tree))
        restoreExpand()
    }

    override fun dispose() {
        ArgumentsService.getInstance(project).model.removeTreeModelListener(this)
    }

    fun getTitleActions(): List<AnAction> {
        return listOf(ActionManager.getInstance().getAction("cmdlineargs.toolwindow.toolbar"))
    }

    fun getExtraActions(): ActionGroup {
        return ActionManager.getInstance().getAction("cmdlineargs.toolwindow.toolbar.ex") as ActionGroup
    }

    private fun installActions() {
        val am = ActionManager.getInstance()
        RenamePrevAction().registerCustomShortcutSet(CustomShortcutSet.fromString("UP"), tree)
        RenameNextAction().registerCustomShortcutSet(CustomShortcutSet.fromString("DOWN"), tree)

        val menu = am.getAction("cmdlineargs.toolwindow.popup") as ActionGroup
        tree.componentPopupMenu = am.createActionPopupMenu(ActionPlaces.POPUP, menu).component
    }

    private fun restoreExpand() {
        ApplicationManager.getApplication().invokeLater {
            tree.expandByPredicate {
                (it.lastPathComponent as? ArgumentTreeNodeBase)?.isExpanded == true
            }
        }
    }

    override fun uiDataSnapshot(sink: DataSink) {
        super.uiDataSnapshot(sink)
        context.treeIsEditing = tree.isEditing
        sink[ArgumentDataContext.KEY] = context.takeIf { !it.disposed }
        sink[PlatformDataKeys.CUT_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.COPY_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.PASTE_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.DELETE_ELEMENT_PROVIDER] = copyPasteProvider
    }

    override fun treeNodesChanged(e: TreeModelEvent?) = Unit
    override fun treeNodesInserted(e: TreeModelEvent?) = Unit
    override fun treeNodesRemoved(e: TreeModelEvent?) = Unit
    override fun treeStructureChanged(e: TreeModelEvent?) {
        if (e?.treePath?.lastPathComponent === tree.model.root) {
            restoreExpand()
        }
    }
}