package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.resources.ActionMessages
import com.github.rebel000.cmdlineargs.tree.ConfigurationNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.project.DumbAwareToggleAction

internal class TrustActionGroup : DefaultActionGroup() {
    init {
        add(TrustByName())
        add(TrustByType())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = false
        e.withArgumentDataContext {
            e.presentation.isVisible = treeSelectedExperimental > 0
            e.presentation.isEnabled = e.presentation.isVisible && treeSelectedCount == treeSelectedConfigurations
        }
    }

    abstract class TrustActionBase : DumbAwareToggleAction() {
        protected abstract fun setTrusted(adapter: ArgumentsAdapter, value: Boolean)
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        override fun setSelected(e: AnActionEvent, state: Boolean) = e.withArgumentDataContext {
            for (path in tree.selectionPaths) {
                (path.lastPathComponent as? ConfigurationNode)?.let { node ->
                    node.settingsID.let { service.findAdapter(it) }
                        ?.let { adapter ->
                            setTrusted(adapter, state)
                            node.isChecked = false
                            node.isEnabled = adapter.isTrusted()
                            node.trusted = adapter.isTrusted()
                            model.invalidate(node, false)
                        }
                }
            }
            service.onConfigurationsVisibilityChanged(true)
        }
    }

    private class TrustByName : TrustActionBase() {
        init {
            templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.trust.name.text")
        }
        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { treeIsTrustedByName }
        override fun setTrusted(adapter: ArgumentsAdapter, value: Boolean) = adapter.setTrustedByName(value)
    }

    private class TrustByType : TrustActionBase() {
        init {
            templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.trust.type.text")
        }
        override fun isSelected(e: AnActionEvent): Boolean = e.withArgumentDataContext(false) { treeIsTrustedByType }
        override fun setTrusted(adapter: ArgumentsAdapter, value: Boolean) = adapter.setTrustedByType(value)
    }
}