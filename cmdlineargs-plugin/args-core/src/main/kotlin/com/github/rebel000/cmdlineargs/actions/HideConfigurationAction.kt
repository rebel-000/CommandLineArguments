package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ConfigurationNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.project.DumbAwareToggleAction

internal class HideConfigurationAction : DumbAwareToggleAction() {
    init {
        templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { treeSelectedHidden > 0 }

    override fun setSelected(e: AnActionEvent, state: Boolean) = e.withArgumentDataContext {
        val visible = !state
        var hiddenCount = 0
        for (path in tree.selectionPaths) {
            (path.lastPathComponent as? ConfigurationNode)?.let { node ->
                if (node.visible != visible) {
                    node.isChecked = false
                    node.visible = visible
                    model.invalidate(node, false)
                }
                if (!node.visible) {
                    hiddenCount++
                }
            }
        }
        treeSelectedHidden = hiddenCount
        service.onConfigurationsVisibilityChanged(false)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.withArgumentDataContext(false) {
            treeSelectedConfigurations > 0 && treeSelectedCount == treeSelectedConfigurations
        }
        super.update(e)
    }
}