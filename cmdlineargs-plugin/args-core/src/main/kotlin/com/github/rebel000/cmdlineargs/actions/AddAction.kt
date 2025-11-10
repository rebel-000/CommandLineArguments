package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.tree.TreePath

internal class AddAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        val model = tree.model
        val node = ArgumentNode("")
        model.add(node, tree.selectedNode())
        if (tree.isEditing) {
            tree.stopEditing()
        }
        val path = TreePath(node.path)
        tree.selectionPaths = arrayOf(path)
        tree.startEditingAtPath(path)
    }
}