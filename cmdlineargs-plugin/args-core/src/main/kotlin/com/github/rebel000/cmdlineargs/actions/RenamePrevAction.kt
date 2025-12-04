package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

internal class RenamePrevAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        val node = (context.tree.editingPath?.lastPathComponent) as ArgumentNode?
        val parent = node?.parent
        if (parent !is ArgumentTreeNodeBase) {
            context.tree.stopEditing()
            return@withArgumentDataContext
        }
        val sibling = node.previousSibling
        if (sibling is ArgumentNode){
            if (sibling.isFolder && sibling.isExpanded && sibling.childCount > 0) {
                val lastChild = sibling.lastChild
                if (lastChild is ArgumentNode) {
                    rename(context.tree, lastChild)
                    return@withArgumentDataContext
                }
            }
        }
        val index = parent.getIndex(node) - 1
        if (index < 0) {
            rename(context.tree, parent)
            return@withArgumentDataContext
        }
        rename(context.tree, sibling)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) { it.treeIsEditing }
    }

    private fun rename(tree: ArgumentTree, node: DefaultMutableTreeNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}