package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

@Suppress("DuplicatedCode")
internal class RenamePrevAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (!tree.isEditing) {
            return
        }
        val node = (tree.editingPath?.lastPathComponent) as ArgumentNode?
        val parent = node?.parent
        if (parent !is ArgumentTreeNodeBase) {
            tree.stopEditing()
            return
        }
        val sibling = node.previousSibling
        if (sibling is ArgumentNode){
            if (sibling.isFolder && sibling.isExpanded && sibling.childCount > 0) {
                val lastChild = sibling.lastChild
                if (lastChild is ArgumentNode) {
                    rename(tree, lastChild)
                    return
                }
            }
        }
        val index = parent.getIndex(node) - 1
        if (index < 0) {
            rename(tree, parent)
            return
        }
        rename(tree, sibling)
    }

    private fun rename(tree: ArgumentTree, node: DefaultMutableTreeNode) {
        tree.stopEditing()
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}