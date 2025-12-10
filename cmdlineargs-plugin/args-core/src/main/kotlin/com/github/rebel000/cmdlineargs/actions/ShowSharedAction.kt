package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ui.SharedWarningDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

internal class ShowSharedAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean = e.myService?.showSharedArguments == true

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val service = e.myService ?: return
        if (state && !SharedWarningDialog().showAndGet()) {
            return
        }
        service.showSharedArguments = state
    }
}