package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent

internal class EditAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (tree.isEditing) {
            tree.stopEditing()
        }
        val node = tree.selectedNode() as? ArgumentNode ?: return
        tree.editNode(node)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (e.presentation.isEnabled) {
            val tree = ArgumentTree.getInstance(e.project) ?: return
            e.presentation.isEnabled = !tree.isEditing && tree.selectedNode() is ArgumentNode
        }
    }
}