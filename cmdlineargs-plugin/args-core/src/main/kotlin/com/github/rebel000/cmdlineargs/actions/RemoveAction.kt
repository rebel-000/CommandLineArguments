package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlin.collections.forEach

internal class RemoveAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (tree.isEditing) return
        val index: Int? = tree.selectionRows?.firstOrNull()
        tree.selectedNodes<ArgumentNode>().forEach { tree.model.remove(it) }
        tree.addSelectionRow(index ?: return)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val tree = ArgumentTree.getInstance(e.project ?: return) ?: return
        if (tree.isEditorActive) {
            e.presentation.isEnabled = false
        }
    }
}