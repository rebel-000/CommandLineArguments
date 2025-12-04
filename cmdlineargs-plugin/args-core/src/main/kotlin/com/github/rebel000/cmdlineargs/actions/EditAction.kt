package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.ui.PropertiesDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

internal class EditAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        context.tree.stopEditing()
        (context.tree.selectedNode() as? ArgumentNode)?.let { node ->
            if (PropertiesDialog(e.project!!, node).showAndGet()) {
                context.model.invalidate(node, false)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            it.treeSelectedArguments == 1 && it.treeSelectedCount == 1
        } && e.project != null
    }
}