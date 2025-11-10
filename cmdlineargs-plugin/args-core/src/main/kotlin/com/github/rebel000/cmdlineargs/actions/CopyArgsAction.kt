package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.ui.ArgumentsToolWindow
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

internal class CopyArgsAction : DefaultActionGroup(Messages.message("action.cmdlineargs.copy-args.text"), true) {
    override fun update(e: AnActionEvent) {
        val toolWindow = e.dataContext.getData(PlatformDataKeys.TOOL_WINDOW)
        val enabled = toolWindow != null && toolWindow.id == ArgumentsToolWindow.TOOLWINDOW_ID
        e.presentation.isEnabled = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    class CopyArgsForSettings(s: RunnerAndConfigurationSettings) : TreeActionBase(s.name) {
        private val type = s.type.id
        private val name = s.name
        private val project = s.configuration.project
        override fun actionPerformed(e: AnActionEvent) {
            val service = ArgumentsService.getInstance(project)
            val runManager = RunManager.getInstanceIfCreated(project)
            val config = runManager?.findConfigurationByTypeAndName(type, name) ?: return
            CopyPasteManager.getInstance().setContents(StringSelection(service.getArguments(config)))
        }
    }
}