package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.SharedWarningDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

internal class ShowSharedAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean {
        val service = ArgumentsService.getInstanceIfCreated(e.project) ?: return false
        return service.isSharedVisible
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val service = ArgumentsService.getInstanceIfCreated(e.project) ?: return
        if (service.isSharedVisible) {
            service.toggleShared(false)
        } else if (SharedWarningDialog().showAndGet()) {
            service.toggleShared(true)
        }
    }
}