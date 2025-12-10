package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.resources.ActionMessages
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon
import javax.swing.tree.TreePath

internal class NewActionGroup : DefaultActionGroup(), TreeAction {
    init {
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add.text"),
                icon = AllIcons.Diff.GutterCheckBoxSelected,
                folder = false,
                parameter = false,
                single = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-folder.text"),
                icon = AllIcons.Nodes.Folder,
                folder = true,
                parameter = false,
                single = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-folder-parameter.text"),
                icon = AllIcons.Actions.GroupByModuleGroup,
                folder = true,
                parameter = true,
                single = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-choice.text"),
                icon = AllIcons.Nodes.Module,
                folder = true,
                parameter = false,
                single = true
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-choice-parameter.text"),
                icon = AllIcons.Actions.GroupByModule,
                folder = true,
                parameter = true,
                single = true
            )
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            treeSelectedContainers > 0 && treeSelectedCount == treeSelectedContainers
        }
        e.presentation.isVisible = e.presentation.isEnabled || !e.isFromContextMenu
    }

    class AddAction(
        text: String?,
        icon: Icon?,
        val folder: Boolean,
        val parameter: Boolean,
        val single: Boolean
    ) : DumbAwareAction(text, null, icon), TreeAction {
        override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext {
            tree.stopEditing()
            ArgumentNode("")
                .apply {
                    isFolder = folder
                    isParameter = parameter
                    isSingle = single
                    model.add(this, tree.selectedNode())
                }
                .let { TreePath(it.path) }
                .let {
                    tree.selectionPaths = arrayOf(it)
                    tree.startEditingAtPath(it)
                }
        }
    }
}