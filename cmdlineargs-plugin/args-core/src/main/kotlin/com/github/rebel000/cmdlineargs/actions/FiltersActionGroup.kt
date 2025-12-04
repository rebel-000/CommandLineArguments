package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction

internal class FiltersActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return ArgumentsService.getInstance(e?.project ?: return EMPTY_ARRAY)
            .getFilters()
            .map { filter ->
                DefaultActionGroup().apply {
                    isPopup = true
                    isSearchable = false
                    templatePresentation.text = filter.title
                    templatePresentation.description = filter.desc
                    for (value in filter.values) {
                        val action = FilterAction(filter.key, value)
                        action.templatePresentation.text = value
                        add(action)
                    }
                }
            }.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.withArgumentDataContext(false) {
            it.treeSelectedArguments > 0 && it.treeSelectedCount == it.treeSelectedArguments
        }
    }

    class FilterAction(val key: String, val value: String) : DumbAwareToggleAction() {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { context ->
            context.tree.anySelectedNodeNoRecursion<ArgumentNode> {
                it.filters[key]?.contains(value) == true
            }
        }

        override fun setSelected(e: AnActionEvent, selected: Boolean) = e.withArgumentDataContext { context ->
            context.tree.forEachSelectedNodeNoRecursion<ArgumentNode> {
                if (selected) {
                    it.addFilter(key, value)
                } else {
                    it.removeFilter(key, value)
                }
                context.model.invalidate(it, false)
            }
        }
    }
}