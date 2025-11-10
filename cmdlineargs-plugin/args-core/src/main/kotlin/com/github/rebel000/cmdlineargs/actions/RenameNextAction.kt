package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath

@Suppress("DuplicatedCode")
internal class RenameNextAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (!tree.isEditing) {
            return
        }
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
        val parentSibling = parent.nextSibling
        if (parentSibling is ArgumentNode) {
            rename(tree, parentSibling)
            return
        }
        if (parent is ArgumentContainer) {
            val newNode = ArgumentNode("")
            tree.model.tryInsertAfter(newNode, parent)
            rename(tree, newNode)
            return
        }
        tree.stopEditing()
    }

    private fun rename(tree: ArgumentTree, node: ArgumentNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}