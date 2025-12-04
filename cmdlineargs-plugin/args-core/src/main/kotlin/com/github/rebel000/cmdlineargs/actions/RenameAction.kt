package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class RenameAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        context.tree.stopEditing()
        context.tree.selectedNode()?.let { node ->
            val path = TreePath(node.path)
            context.tree.selectionPaths = arrayOf(path)
            context.tree.startEditingAtPath(path)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            it.treeSelectedArguments > 0 && it.treeSelectedCount == it.treeSelectedArguments
        }
    }
}