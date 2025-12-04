package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterName
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection

internal class CopyCommandLineActionGroup : DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return e?.project?.let { ArgumentsService.getInstance(it).copyArgsActions } ?: EMPTY_ARRAY
    }

    class Action(s: RunnerAndConfigurationSettings) : DumbAwareAction(s.getArgumentsAdapterName()) {
        private val type = s.type.id
        private val name = s.name
        override fun actionPerformed(e: AnActionEvent) {
            RunManager.getInstanceIfCreated(e.project ?: return)
                ?.findConfigurationByTypeAndName(type, name)
                ?.let { config ->
                    val value = ArgumentsService.getInstance(e.project!!).getArguments(config)
                    CopyPasteManager.getInstance().setContents(StringSelection(value))
                }
        }
    }
}