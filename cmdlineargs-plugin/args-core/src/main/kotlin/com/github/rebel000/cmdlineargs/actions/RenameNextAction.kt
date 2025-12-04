package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class RenameNextAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        val node = (context.tree.editingPath?.lastPathComponent) as? ArgumentNode
        val parent = node?.parent
        if (parent !is ArgumentTreeNodeBase) {
            context.tree.stopEditing()
            return@withArgumentDataContext
        }
        if (node.isFolder && node.isExpanded && node.childCount > 0) {
            val firstChild = node.getChildAt(0)
            if (firstChild is ArgumentNode) {
                rename(context.tree, firstChild)
                return@withArgumentDataContext
            }
        }
        val nodeSibling = node.nextSibling
        if (nodeSibling is ArgumentNode) {
            rename(context.tree, nodeSibling)
            return@withArgumentDataContext
        }
        val parentSibling = parent.nextSibling
        if (parentSibling is ArgumentNode) {
            rename(context.tree, parentSibling)
            return@withArgumentDataContext
        }
        if (parent is ArgumentContainer) {
            val newNode = ArgumentNode("")
            context.model.tryInsertAfter(newNode, parent)
            rename(context.tree, newNode)
            return@withArgumentDataContext
        }
        context.tree.stopEditing()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) { it.treeIsEditing }
    }

    private fun rename(tree: ArgumentTree, node: ArgumentNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}