package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class MoveUpAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
        val selectedNodes = context.tree.selectedNodesNoRecursion<ArgumentNode>()
        if (selectedNodes.isEmpty()) {
            return@withArgumentDataContext
        }
        var anchorNode: ArgumentContainer = selectedNodes.first()
        var parent: ArgumentContainer = anchorNode.parent as? ArgumentContainer ?: return@withArgumentDataContext
        var index = parent.getIndex(anchorNode) - 1
        if (index == -2) {
            return@withArgumentDataContext
        }
        if (index == -1) {
            anchorNode = parent
            parent = parent.parent as? ArgumentContainer ?: return@withArgumentDataContext
        }
        val readonly = anchorNode !is ArgumentNode
        if (index != -1 || readonly) {
            val neighbor = parent.getChildBefore(anchorNode) as ArgumentTreeNodeBase?
            if (neighbor is ArgumentNode && neighbor.isFolder) {
                parent = neighbor
                index = neighbor.childCount
            } else if (readonly) {
                return@withArgumentDataContext
            }
        }
        else {
            index = parent.getIndex(anchorNode)
        }
        val newSelectionPaths = ArrayList<TreePath>(selectedNodes.count())
        for (node in selectedNodes) {
            val wasExpanded = context.tree.isExpanded(TreePath(node.path))
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