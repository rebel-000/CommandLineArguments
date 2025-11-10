package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.*
import com.intellij.openapi.actionSystem.*

@Suppress("DuplicatedCode")
class ShowSharedAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        return ArgumentsService.getInstanceIfCreated(e.project)?.isSharedVisible == true
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val argsService = ArgumentsService.getInstanceIfCreated(e.project) ?: return
        if (argsService.isSharedVisible) {
            argsService.toggleShared(false)
        } else if (SharedWarningDialog(argsService).showAndGet()) {
            argsService.toggleShared(true)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)?.id == ArgumentsToolWindow.TOOLWINDOW_ID
        Toggleable.setSelected(e.presentation, isSelected(e))
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}