package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.getQualifiedDisplayName
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection

internal class CopyCommandLineActionGroup : DefaultActionGroup() {
    private var revision = -1

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = false
        e.myService?.let { service ->
            if (revision != service.revision) {
                revision = service.revision
                removeAll()
                RunManager.getInstanceIfCreated(e.project!!)
                    ?.allSettings
                    ?.forEach { s ->
                        if (service.getAdapter(s) != null) {
                            addAction(Action(s))
                        }
                    }
            }
            e.presentation.isEnabled = childrenCount > 0
        }
    }

    class Action(s: RunnerAndConfigurationSettings) : DumbAwareAction(s.getQualifiedDisplayName()) {
        private val type = s.type.id
        private val name = s.name
        override fun actionPerformed(e: AnActionEvent) {
            e.myService?.let { service ->
                RunManager.getInstanceIfCreated(service.project)
                    ?.findConfigurationByTypeAndName(type, name)
                    ?.let { CopyPasteManager.getInstance().setContents(StringSelection(service.getArguments(it))) }
            }
        }
    }
}