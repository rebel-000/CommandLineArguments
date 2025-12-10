package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.ui.PropertiesDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

internal class EditAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        tree.stopEditing()
        (tree.selectedNode() as? ArgumentNode)?.let { node ->
            if (PropertiesDialog(e.project!!, node).showAndGet()) {
                model.invalidate(node, false)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.withArgumentDataContext(false) {
            treeSelectedArguments == 1 && treeSelectedCount == 1
        }
    }
}