package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.openapi.actionSystem.AnActionEvent

internal class ReloadAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        ArgumentsService.getInstance(e.project ?: return).reload()
    }
}