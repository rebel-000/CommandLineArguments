package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction

internal class QuickParametersActionGroup : DefaultActionGroup() {
    init {
        add(FolderToggle())
        add(ParameterToggle())
        add(ChoiceToggle())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.withArgumentDataContext(false) {
            it.treeSelectedArguments == 1 && it.treeSelectedCount == 1
        }
    }

    private class FolderToggle : DumbAwareToggleAction() {
        init {
            templatePresentation.text = Messages.message("properties.isFolder")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.isFolder ?: false
        }

        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.withArgumentDataContext { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.let {
                it.isFolder = enabled
                it.isParameter = it.isParameter && enabled
                it.isSingle = it.isSingle && enabled
                context.model.invalidate(it, false)
            }
        }
    }

    private class ParameterToggle : DumbAwareToggleAction() {
        init {
            templatePresentation.text = Messages.message("properties.isParameter")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.let {
                it.isFolder && it.isParameter
            } ?: false
        }

        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.withArgumentDataContext { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.let {
                it.isFolder = it.isFolder || enabled
                it.isParameter = enabled
                context.model.invalidate(it, false)
            }
        }
    }

    private class ChoiceToggle : DumbAwareToggleAction() {
        init {
            templatePresentation.text = Messages.message("properties.isSingle")
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.let {
                it.isFolder && it.isSingle
            } ?: false
        }

        override fun setSelected(e: AnActionEvent, enabled: Boolean) = e.withArgumentDataContext { context ->
            (context.tree.selectedNode() as? ArgumentNode)?.let {
                it.isFolder = it.isFolder || enabled
                it.isSingle = enabled
                context.model.invalidate(it, false)
            }
        }
    }
}