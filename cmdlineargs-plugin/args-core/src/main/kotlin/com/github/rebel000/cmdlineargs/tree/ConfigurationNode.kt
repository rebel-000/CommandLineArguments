package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.getQualifiedDisplayName
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase.Companion.ControlType
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.icons.AllIcons
import javax.swing.Icon

internal open class ConfigurationNode(adapter: ArgumentsAdapter?, settings: RunnerAndConfigurationSettings) : ArgumentTreeNodeBase("<error>") {
    companion object {
        const val MAX_VALUE_LENGTH = 256
        const val MAX_TOOLTIP_LENGTH = 1152
    }

    private var _value: String = ""

    var experimental: Boolean = false
    var paused: Boolean = false
    var settingsID: String
    var trusted: Boolean = false
    var visible: Boolean = true

    var active: Boolean
        get() = style != null
        set(value) {
            style = when {
                value && isEnabled -> SUCCESS_TEXT_ATTRIBUTES
                value -> WARN_TEXT_ATTRIBUTES
                else -> null
            }
        }

    var value: String
        get() = _value
        set(value) {
            if (isEnabled) {
                tooltip = if (experimental) {
                    Messages.message("tooltip.untrusted")
                } else {
                    value.trimTo(MAX_TOOLTIP_LENGTH)
                }
                _value = value.trimTo(MAX_VALUE_LENGTH)
            }
        }

    override val controlType get() = when {
        trusted && visible -> ControlType.CHECKBOX
        !visible || experimental -> ControlType.CHECKBOX_LOCKED
        else -> ControlType.NONE
    }

    override var icon: Icon?
        get() = when {
            !visible -> AllIcons.Actions.Unshare
            !trusted && !experimental -> AllIcons.Run.ShowIgnored
            !trusted -> AllIcons.General.ShowWarning
            !isChecked || paused -> AllIcons.Actions.Pause
            else -> super.icon
        }
        set(value) {
            super.icon = value
        }

    init {
        icon = settings.configuration.icon
        settingsID = settings.uniqueID
        text = settings.getQualifiedDisplayName()
        if (adapter != null) {
            isChecked = adapter.enabled
            isEnabled = adapter.isTrusted()
            experimental = adapter.isExperimental()
            trusted = adapter.isTrusted()
            value = adapter.getArguments()
        } else {
            isChecked = false
            isEnabled = false
            experimental = false
            trusted = false
            _value = Messages.message("toolwindow.notSupportedNode")
            tooltip = null
        }
    }

    private fun String.trimTo(len: Int): String {
        return if (length > len) {
            "${take(len)}..."
        } else {
            this
        }
    }
}
