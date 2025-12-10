package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ui.ArgumentDataContext
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil.copyFrom
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.tree.TreeUtil

internal class CollapseAllAction : DumbAwareAction(), TreeAction {
    init {
        copyFrom(this, IdeActions.ACTION_COLLAPSE_ALL)
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        TreeUtil.collapseAll(tree, 1)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(ArgumentDataContext.KEY) != null
    }
}