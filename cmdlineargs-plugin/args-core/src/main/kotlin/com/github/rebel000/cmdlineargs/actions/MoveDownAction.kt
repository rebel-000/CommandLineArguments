package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath
import kotlin.math.min

internal class MoveDownAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        val selectedNodes = context.tree.selectedNodesNoRecursion<ArgumentNode>()
        var anchor: ArgumentTreeNodeBase = selectedNodes[selectedNodes.count() - 1]
        var parent = anchor.parent as? ArgumentContainer ?: return@withArgumentDataContext
        var index = parent.getIndex(anchor) + 1
        if (index == 0) {
            return@withArgumentDataContext
        }
        val last = (index == parent.childCount)
        if (last) {
            anchor = parent
            parent = parent.parent as? ArgumentContainer ?: return@withArgumentDataContext
        }
        val readonly = anchor !is ArgumentNode
        if (!last || readonly) {
            val sibling = parent.getChildAfter(anchor) as ArgumentTreeNodeBase?
            if (sibling is ArgumentNode && sibling.isFolder) {
                parent = sibling
                index = -1
            } else if (readonly) {
                return@withArgumentDataContext
            }
        }
        else {
            index = parent.getIndex(anchor)
        }
        ++index
        index = min(index, parent.childCount)
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = context.tree.isExpanded(TreePath(node.path))
            if (node.parent === parent && parent.getIndex(node) < index) {
                index--
            }
            context.model.remove(node)
            context.model.insert(node, parent, index)
            if (parent is ArgumentNode && parent.isSingle) {
                context.tree.setNodeState(node, false)
            }
            val path = TreePath(node.path)
            newSelectionPaths.add(path)
            if (wasExpanded) {
                context.tree.expandPath(path)
            }
            index++
        }
        context.tree.selectionPaths = newSelectionPaths.toArray(arrayOf())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.withArgumentDataContext(false) {
            it.treeSelectedArguments > 0 && it.treeSelectedCount == it.treeSelectedArguments
        }
    }
}