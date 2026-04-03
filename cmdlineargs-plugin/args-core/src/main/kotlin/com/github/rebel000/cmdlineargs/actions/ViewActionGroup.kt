package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.resources.ActionMessages
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction

internal class ViewActionGroup : DefaultActionGroup() {
    init {
        add(ShowHiddenAction())
        add(ShowExperimentalAction())
        add(ShowUnsupportedAction())
    }

    private class ShowHiddenAction : DumbAwareToggleAction() {
        init {
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.view.show-hidden.text")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        override fun isSelected(e: AnActionEvent): Boolean = e.myService?.showHidden == true
        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.myService?.showHidden = enabled
    }

    private class ShowExperimentalAction : DumbAwareToggleAction() {
        init {
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.view.show-experimental.text")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        override fun isSelected(e: AnActionEvent): Boolean = e.myService?.showExperimental == true
        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.myService?.showExperimental = enabled
    }

    private class ShowUnsupportedAction : DumbAwareToggleAction() {
        init {
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.view.show-unsupported.text")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        override fun isSelected(e: AnActionEvent): Boolean = e.myService?.showNotSupported == true
        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.myService?.showNotSupported = enabled
    }
}