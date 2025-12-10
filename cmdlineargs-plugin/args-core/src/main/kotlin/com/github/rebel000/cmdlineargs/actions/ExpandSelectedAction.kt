package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase
import com.github.rebel000.cmdlineargs.tree.traverse
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil.copyFrom
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.tree.TreePath

internal class ExpandSelectedAction : DumbAwareAction(), TreeAction {
    init {
        copyFrom(this, IdeActions.ACTION_EXPAND_ALL)
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        tree.forEachSelectedNodeNoRecursion<ArgumentTreeNodeBase> { node ->
            node.traverse<ArgumentTreeNodeBase> {
                tree.expandPath(TreePath(it.path))
                true
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            treeSelectedCount > 0
        }
    }
}