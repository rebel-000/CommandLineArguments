package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.intellij.ide.CommonActionsManager
import com.intellij.ide.DefaultTreeExpander
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.util.ui.JBUI
import java.util.LinkedList

internal class ArgumentsToolWindow(project: Project) : SimpleToolWindowPanel(true) {
    companion object {
        const val TOOLWINDOW_ID = "cmdlineargs"
        const val TOOLBAR_ACTION_ID = "cmdlineargs.actions"
    }

    val tree = ArgumentTree(project)

    init {
        val transferHandler = tree.transferHandler
        add(ToolbarDecorator.createDecorator(tree).apply {
            setToolbarPosition(ActionToolbarPosition.TOP)
            setPanelBorder(JBUI.Borders.empty())
            addExtraActions(ActionManager.getInstance().getAction(TOOLBAR_ACTION_ID))
        }.setForcedDnD().createPanel())
        tree.transferHandler = transferHandler
    }

    fun getTitleActions(): List<AnAction> {
        val treeExpander = DefaultTreeExpander(tree)
        val titleActions = LinkedList<AnAction>()
        titleActions.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, tree))
        titleActions.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, tree))
        return titleActions
    }
}