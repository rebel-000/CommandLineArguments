package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.traverse
import com.github.rebel000.cmdlineargs.tree.visitors.CollectCopyVisitor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.StringSelection

internal class CopyAsPlainTextAction : DumbAwareAction(), TreeAction {
    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context -> 
        val visitor = CollectCopyVisitor()
        context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
            it.traverse(visitor)
        }
        val value = visitor.toString()
        if (value.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(value))
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = false
        e.withArgumentDataContext {
            e.presentation.isVisible = it.treeSelectedContainers > 0
            e.presentation.isEnabled = e.presentation.isVisible && it.treeSelectedConfigurations == 0 && !it.treeIsEditing
        }
    }
}