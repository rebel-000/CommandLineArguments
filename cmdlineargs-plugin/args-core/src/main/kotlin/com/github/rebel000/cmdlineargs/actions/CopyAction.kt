package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.github.rebel000.cmdlineargs.tree.visitors.CollectCopyVisitor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

internal class CopyAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (tree.isEditing) return
        val visitor = CollectCopyVisitor()
        for (node in tree.selectedNodes<ArgumentContainer>()) {
            node.traverse(visitor)
        }
        val value = visitor.toString()
        if (value.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(value))
        }
    }

}
