package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class RenameAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        tree.stopEditing()
        tree.selectedNode()?.let { node ->
            val path = TreePath(node.path)
            tree.selectionPaths = arrayOf(path)
            tree.startEditingAtPath(path)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            treeSelectedArguments > 0 && treeSelectedCount == treeSelectedArguments
        }
    }
}