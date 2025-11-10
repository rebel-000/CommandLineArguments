package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.ArgumentsToolWindow
import com.intellij.openapi.actionSystem.*

internal class DisableAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return !ArgumentsService.getInstance(project).isEnabled
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val argsService = ArgumentsService.getInstance(e.project ?: return)
        argsService.isEnabled = !state
    }

    override fun update(e: AnActionEvent) {
        val argsService = ArgumentsService.getInstance(e.project ?: return)
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        Toggleable.setSelected(e.presentation, !argsService.isEnabled)
        e.presentation.isEnabled = toolWindow != null && toolWindow.id == ArgumentsToolWindow.TOOLWINDOW_ID
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}