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
                isFolder = false,
                isParameter = false,
                isSingle = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-folder.text"),
                icon = AllIcons.Nodes.Folder,
                isFolder = true,
                isParameter = false,
                isSingle = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-folder-parameter.text"),
                icon = AllIcons.Actions.GroupByModuleGroup,
                isFolder = true,
                isParameter = true,
                isSingle = false
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-choice.text"),
                icon = AllIcons.Nodes.Module,
                isFolder = true,
                isParameter = false,
                isSingle = true
            )
        )
        add(
            AddAction(
                text = ActionMessages.message("action.cmdlineargs.add-choice-parameter.text"),
                icon = AllIcons.Actions.GroupByModule,
                isFolder = true,
                isParameter = true,
                isSingle = true
            )
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.withArgumentDataContext(false) {
            it.treeSelectedContainers > 0 && it.treeSelectedCount == it.treeSelectedContainers && !it.treeIsEditing
        }
        e.presentation.isVisible = e.presentation.isEnabled || !e.isFromContextMenu
    }

    class AddAction(
        text: String?,
        icon: Icon?,
        val isFolder: Boolean,
        val isParameter: Boolean,
        val isSingle: Boolean
    ) : DumbAwareAction(text, null, icon), TreeAction {
        override fun actionPerformed(e: AnActionEvent) = e.withArgumentDataContext { context ->
            val node = ArgumentNode("")
            node.isFolder = isFolder
            node.isParameter = isParameter
            node.isSingle = isSingle
            context.model.add(node, context.tree.selectedNode())
            context.tree.stopEditing()
            val path = TreePath(node.path)
            context.tree.selectionPaths = arrayOf(path)
            context.tree.startEditingAtPath(path)
        }
    }
}