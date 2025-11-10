package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath

@Suppress("DuplicatedCode")
internal class MoveUpAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val selectedNodes = tree.selectedNodes<ArgumentNode>()
        if (selectedNodes.isEmpty()) {
            return
        }
        var anchorNode: ArgumentContainer = selectedNodes.first()
        var parent: ArgumentContainer = anchorNode.parent as? ArgumentContainer ?: return
        var index = parent.getIndex(anchorNode) - 1
        if (index == -2) {
            return
        }
        if (index == -1) {
            anchorNode = parent
            parent = parent.parent as? ArgumentContainer ?: return
        }
        val readonly = anchorNode !is ArgumentNode
        if (index != -1 || readonly) {
            val neighbor = parent.getChildBefore(anchorNode) as ArgumentTreeNodeBase?
            if (neighbor is ArgumentNode && neighbor.isFolder) {
                parent = neighbor
                index = neighbor.childCount
            } else if (readonly) {
                return
            }
        }
        else {
            index = parent.getIndex(anchorNode)
        }
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = tree.isExpanded(TreePath(node.path))
            tree.model.remove(node)
            tree.model.insert(node, parent, index)
            if (parent is ArgumentNode && parent.isSingle) {
                tree.setNodeState(node, false)
            }
            val path = TreePath(node.path)
            newSelectionPaths.add(path)
            if (wasExpanded) {
                tree.expandPath(path)
            }
            index++
        }
        tree.selectionPaths = newSelectionPaths.toArray(arrayOf())
    }
}