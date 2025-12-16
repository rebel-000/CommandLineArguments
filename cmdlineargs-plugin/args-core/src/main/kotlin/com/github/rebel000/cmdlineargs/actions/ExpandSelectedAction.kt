package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil.copyFrom
import com.intellij.openapi.project.DumbAwareAction

internal class ExpandSelectedAction : DumbAwareAction(), TreeAction {
    init {
        copyFrom(this, IdeActions.ACTION_EXPAND_ALL)
        isEnabledInModalContext = true
    }

    override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
        tree.selectionPaths?.let { selection ->
            tree.expandByPredicate { path ->
                selection.any {
                    it == path || it.isDescendant(path) || path.isDescendant(it)
                }
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