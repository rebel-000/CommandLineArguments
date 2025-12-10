package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.ArgumentsService
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
            e.presentation.isVisible = it.treeSelectedExperimental > 0
            e.presentation.isEnabled = e.presentation.isVisible && it.treeSelectedCount == it.treeSelectedConfigurations
        }
    }

    abstract class TrustActionBase : DumbAwareToggleAction() {
        private var selected: Boolean = false

        protected abstract fun isTrusted(adapter: ArgumentsAdapter): Boolean?
        protected abstract fun setTrusted(adapter: ArgumentsAdapter, value: Boolean)

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean {
            return selected
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            var updatePreview = false
            val service = ArgumentsService.getInstanceIfCreated(e.project) ?: return
            val selectionPaths = e.getArgumentDataContext()?.tree?.selectionPaths ?: return
            for (path in selectionPaths) {
                val node = path.lastPathComponent as? ConfigurationNode
                val key = node?.key ?: continue
                val adapter = service.findAdapter(key) ?: continue
                setTrusted(adapter, state)
                node.isEnabled = adapter.isTrusted()
                updatePreview = true
            }
            selected = state
            if (updatePreview) {
                service.updatePreview()
                service.markDirty()
            }
        }

        override fun update(e: AnActionEvent) {
            val isTrusted: Boolean? = isTrusted(e)
            e.presentation.isEnabledAndVisible = isTrusted != null
            selected = isTrusted == true
            super.update(e)
        }

        private fun isTrusted(e: AnActionEvent): Boolean? {
            var hasTrusted = false
            val service = ArgumentsService.getInstanceIfCreated(e.project) ?: return null
            val selectionPaths = e.dataContext.getArgumentDataContext()?.tree?.selectionPaths.orEmpty()
            for (path in selectionPaths) {
                val key = (path.lastPathComponent as? ConfigurationNode)?.key ?: return null
                val adapter = service.findAdapter(key) ?: return null
                val trusted = isTrusted(adapter)
                if (trusted == false) return false
                hasTrusted = hasTrusted || trusted == true
            }
            return if (hasTrusted) true else null
        }
    }

    private class TrustByName : TrustActionBase() {
        init {
            templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.trust.name.text")
        }
        override fun isTrusted(adapter: ArgumentsAdapter): Boolean? = adapter.isTrustedByName()
        override fun setTrusted(adapter: ArgumentsAdapter, value: Boolean) = adapter.setTrustedByName(value)
    }

    private class TrustByType : TrustActionBase() {
        init {
            templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
            templatePresentation.text = ActionMessages.message("action.cmdlineargs.trust.type.text")
        }
        override fun isTrusted(adapter: ArgumentsAdapter): Boolean? = adapter.isTrustedByType()
        override fun setTrusted(adapter: ArgumentsAdapter, value: Boolean) = adapter.setTrustedByType(value)
    }
}