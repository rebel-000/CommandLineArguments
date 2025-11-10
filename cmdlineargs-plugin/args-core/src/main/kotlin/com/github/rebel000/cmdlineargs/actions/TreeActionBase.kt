package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ui.ArgumentsToolWindow
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

abstract class TreeActionBase(text: String? = null) : AnAction(text) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)?.id == ArgumentsToolWindow.TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}