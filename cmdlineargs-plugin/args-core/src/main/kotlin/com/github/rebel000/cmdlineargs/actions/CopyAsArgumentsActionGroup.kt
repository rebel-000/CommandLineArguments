package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterName
import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.traverse
import com.github.rebel000.cmdlineargs.tree.visitors.CollectArgsVisitor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection

internal class CopyAsArgumentsActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val service = e?.getArgumentsService() ?: return emptyArray()
        val allSettings = RunManager.getInstanceIfCreated(e.project!!)?.allSettings.orEmpty()
        return allSettings.mapNotNull {
            if (service.getAdapter(it)?.isTrusted() == true) {
                MyAction(it)
            } else {
                null
            }
        }.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = false
        e.withArgumentDataContext {
            e.presentation.isVisible = it.treeSelectedContainers > 0
            e.presentation.isEnabled = e.presentation.isVisible && it.treeSelectedConfigurations == 0
        }
    }

    class MyAction(s: RunnerAndConfigurationSettings) : DumbAwareAction(s.getArgumentsAdapterName()) {
        private val type = s.type.id
        private val name = s.name
        override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
            context.tree.stopEditing()
            RunManager.getInstance(e.project!!).findConfigurationByTypeAndName(type, name)?.let { config ->
                val visitor = CollectArgsVisitor(context.service.getAdapter(config)?.predicate() ?: { true })
                context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
                    it.traverse(visitor)
                }
                val value = visitor.toString()
                if (value.isNotEmpty()) {
                    CopyPasteManager.getInstance().setContents(StringSelection(value))
                }
            }
        }
    }
}