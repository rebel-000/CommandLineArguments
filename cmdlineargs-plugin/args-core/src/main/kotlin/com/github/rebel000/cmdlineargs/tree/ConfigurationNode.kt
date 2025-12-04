package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterName
import com.github.rebel000.cmdlineargs.resources.Messages
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.icons.AllIcons
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

internal open class ConfigurationNode(text: String, icon: Icon? = null, style: SimpleTextAttributes? = null) : ArgumentTreeNodeBase(text) {
    var key: String? = null
    var isExperimental = false
    var isTrusted = true

    override val controlType: Companion.ControlType get() = when {
        isEnabled -> if (isTrusted) Companion.ControlType.CHECKBOX else Companion.ControlType.CHECKBOX_LOCKED
        else -> Companion.ControlType.NONE
    }

    init {
        isEnabled = false
        this.icon = icon
        this.style = style
    }

    fun setConfiguration(config: RunnerAndConfigurationSettings, adapter: ArgumentsAdapter?, isActive: Boolean, isGlobalEnabled: Boolean) {
        if (adapter != null) {
            isEnabled = true
            isExperimental = adapter.isExperimental()
            isTrusted = adapter.isTrusted()
            isChecked = adapter.enabled
            icon = when {
                isExperimental -> AllIcons.General.ShowWarning
                !isGlobalEnabled || !adapter.enabled -> AllIcons.Actions.Pause
                isActive -> AllIcons.Actions.Execute
                else -> AllIcons.Toolwindows.ToolWindowRun
            }
            key = adapter.key
            style = when {
                isActive -> SUCCESS_TEXT_ATTRIBUTES
                else -> null
            }
            text = "${config.getArgumentsAdapterName()}: ${adapter.getArguments()}"
            tooltip = when {
                isExperimental -> Messages.message("tooltip.untrusted")
                else -> null
            }
        } else {
            isEnabled = false
            isChecked = false
            icon = AllIcons.Run.ShowIgnored
            key = null
            style = when {
                isActive -> WARN_TEXT_ATTRIBUTES
                else -> null
            }
            text = "[${config.type.displayName}] ${config.name}: ${Messages.message("toolwindow.notSupportedNode")}"
            tooltip = null
        }
    }

    override fun toString(): String {
        return text
    }
}
