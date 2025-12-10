package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

internal class DisableAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean {
        return e.myService?.isEnabled != true
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.myService?.isEnabled = !state
    }
}