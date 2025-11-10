package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath
import kotlin.math.min

@Suppress("DuplicatedCode")
internal class MoveDownAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val selectedNodes = tree.selectedNodes<ArgumentNode>()
        if (selectedNodes.isEmpty()) {
            return
        }
        var anchor: ArgumentTreeNodeBase = selectedNodes[selectedNodes.count() - 1]
        var parent = anchor.parent as? ArgumentContainer ?: return
        var index = parent.getIndex(anchor) + 1
        if (index == 0) {
            return
        }
        val last = (index == parent.childCount)
        if (last) {
            anchor = parent
            parent = parent.parent as? ArgumentContainer ?: return
        }
        val readonly = anchor !is ArgumentNode
        if (!last || readonly) {
            val sibling = parent.getChildAfter(anchor) as ArgumentTreeNodeBase?
            if (sibling is ArgumentNode && sibling.isFolder) {
                parent = sibling
                index = -1
            } else if (readonly) {
                return
            }
        }
        else {
            index = parent.getIndex(anchor)
        }
        ++index
        index = min(index, parent.childCount)
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = tree.isExpanded(TreePath(node.path))
            if (node.parent === parent && parent.getIndex(node) < index) {
                index--
            }
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