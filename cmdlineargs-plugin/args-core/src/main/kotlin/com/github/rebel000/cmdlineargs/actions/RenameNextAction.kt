package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class RenameNextAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        val node = (tree.editingPath?.lastPathComponent) as? ArgumentNode
        val parent = node?.parent
        if (parent !is ArgumentTreeNodeBase) {
            tree.stopEditing()
            return
        }
        if (node.isFolder && node.isExpanded && node.childCount > 0) {
            val firstChild = node.getChildAt(0)
            if (firstChild is ArgumentNode) {
                rename(tree, firstChild)
                return
            }
        }
        val nodeSibling = node.nextSibling
        if (nodeSibling is ArgumentNode) {
            rename(tree, nodeSibling)
            return
        }
        tree.stopEditing()
        if (node.text.isNotEmpty()) {
            val newNode = ArgumentNode("")
            model.tryInsertAfter(newNode, node)
            rename(tree, newNode)
            return
        }
        val parentSibling = parent.nextSibling
        if (parentSibling is ArgumentNode) {
            rename(tree, parentSibling)
            return
        }
        if (parent is ArgumentNode) {
            val newNode = ArgumentNode("")
            model.tryInsertAfter(newNode, parent)
            rename(tree, newNode)
            return
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) { treeIsEditing }
    }

    private fun rename(tree: ArgumentTree, node: ArgumentNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}