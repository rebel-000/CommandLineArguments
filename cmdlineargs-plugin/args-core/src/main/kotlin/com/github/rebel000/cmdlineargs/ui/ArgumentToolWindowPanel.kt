package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.actions.CopyPasteProvider
import com.github.rebel000.cmdlineargs.actions.RenameNextAction
import com.github.rebel000.cmdlineargs.actions.RenamePrevAction
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeDnDSupport
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory

internal class ArgumentToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true), Disposable {
    private val context = ArgumentDataContext()
    private val copyPasteProvider = CopyPasteProvider()
    private val tree = ArgumentTree(ArgumentsService.getInstance(project).model)

    init {
        add(ScrollPaneFactory.createScrollPane(tree))
        ArgumentTreeDnDSupport(tree).install(this)
        context.install(ArgumentsService.getInstance(project), tree)
        installActions()
    }

    private fun installActions() {
        val am = ActionManager.getInstance()
        RenamePrevAction().registerCustomShortcutSet(CustomShortcutSet.fromString("UP"), tree)
        RenameNextAction().registerCustomShortcutSet(CustomShortcutSet.fromString("DOWN"), tree)

        val menu = am.getAction("cmdlineargs.toolwindow.popup") as ActionGroup
        tree.componentPopupMenu = am.createActionPopupMenu(ActionPlaces.POPUP, menu).component
    }

    override fun dispose() {
        context.uninstall()
    }

    override fun uiDataSnapshot(sink: DataSink) {
        super.uiDataSnapshot(sink)
        context.treeIsEditing = tree.isEditing
        sink[ArgumentDataContext.KEY] = context
        sink[PlatformDataKeys.CUT_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.COPY_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.PASTE_PROVIDER] = copyPasteProvider
        sink[PlatformDataKeys.DELETE_ELEMENT_PROVIDER] = copyPasteProvider
    }

    fun getTitleActions(): List<AnAction> {
        val treeExpander = DefaultTreeExpander(tree)
        return listOf(
            ActionManager.getInstance().getAction("cmdlineargs.toolwindow.toolbar"),
            Separator(),
            CommonActionsManager.getInstance().createExpandAllAction(treeExpander, tree),
            CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree)
        )
    }

    fun getExtraActions(): ActionGroup {
        return ActionManager.getInstance().getAction("cmdlineargs.toolwindow.toolbar.ex") as ActionGroup
    }
}