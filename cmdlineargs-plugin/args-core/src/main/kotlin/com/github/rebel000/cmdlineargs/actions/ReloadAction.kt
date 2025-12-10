package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

internal class ReloadAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.myService?.reload()
    }
}