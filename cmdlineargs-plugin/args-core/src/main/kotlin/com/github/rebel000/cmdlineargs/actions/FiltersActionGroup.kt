package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareToggleAction

internal class FiltersActionGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return e.myService
            ?.getFilters()
            ?.map { filter ->
                DefaultActionGroup().apply {
                    isPopup = true
                    isSearchable = false
                    templatePresentation.text = filter.title
                    templatePresentation.description = filter.desc
                    for (value in filter.values) {
                        add(FilterAction(filter.key, value))
                    }
                }
            }?.toTypedArray() ?: EMPTY_ARRAY
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.withArgumentDataContext(false) {
            treeSelectedArguments > 0 && treeSelectedCount == treeSelectedArguments
        }
    }

    class FilterAction(val key: String, val value: String) : DumbAwareToggleAction() {
        init {
            templatePresentation.text = value
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) {
            tree.forEachSelectedNodeNoRecursion<ArgumentNode> {
                if (it.filters[key]?.contains(value) == true) {
                    return true
                }
            }
            return false
        }

        override fun setSelected(e: AnActionEvent, selected: Boolean) = e.withArgumentDataContext {
            tree.forEachSelectedNodeNoRecursion<ArgumentNode> {
                if (selected) {
                    it.addFilter(key, value)
                } else {
                    it.removeFilter(key, value)
                }
                model.invalidate(it, false)
            }
        }
    }
}