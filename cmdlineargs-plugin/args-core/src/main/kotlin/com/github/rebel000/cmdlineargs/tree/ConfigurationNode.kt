package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.getQualifiedDisplayName
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.tree.ArgumentTreeNodeBase.Companion.ControlType
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.icons.AllIcons
import javax.swing.Icon

internal open class ConfigurationNode : ArgumentTreeNodeBase("<error>") {
    companion object {
        const val MAX_NAME_LENGTH = 256
        const val MAX_TOOLTIP_LENGTH = 1152
    }

    private var name: String = ""
    private var myIcon: Icon? = null

    private var _controlType: ControlType = ControlType.NONE
    private var isServiceEnabled = false
    private var isTrusted = true

    var isExperimental: Boolean = false
    var settingsID: String? = null

    override val controlType get() = _controlType

    init {
        isEnabled = false
        icon = AllIcons.Run.ShowIgnored
    }

    fun configure(settings: RunnerAndConfigurationSettings, adapter: ArgumentsAdapter?, serviceEnabled: Boolean, showExperimental: Boolean) {
        val adapter = adapter?.takeIf { it.isVisible(showExperimental) }
        settingsID = settings.uniqueID
        _controlType = ControlType.NONE
        if (adapter != null) {
            isEnabled = true
            isChecked = adapter.enabled
            isExperimental = adapter.isExperimental()
            isServiceEnabled = serviceEnabled
            isTrusted = adapter.isTrusted()
            name = settings.getQualifiedDisplayName()
            myIcon = settings.type.icon
            setValue(adapter.getArguments())
            update()
        } else {
            isEnabled = false
            isChecked = false
            isExperimental = false
            isServiceEnabled = false
            isTrusted = false
            name = ""
            myIcon = null
            icon = AllIcons.Run.ShowIgnored
            text = "${settings.getQualifiedDisplayName()}: ${Messages.message("toolwindow.notSupportedNode")}"
            tooltip = null
        }
    }

    fun setActive(isActive: Boolean) {
        style = when {
            isActive && isEnabled -> SUCCESS_TEXT_ATTRIBUTES
            isActive -> WARN_TEXT_ATTRIBUTES
            else -> null
        }
    }

    fun setServiceEnabled(value: Boolean) {
        isServiceEnabled = value
        update()
    }

    fun setTrusted(value: Boolean) {
        isTrusted = value
        update()
    }

    fun setValue(value: String) {
        if (isEnabled) {
            if (isExperimental) {
                text = "*$name: ${value.trimTo(MAX_NAME_LENGTH)}"
                tooltip = Messages.message("tooltip.untrusted")
            } else {
                text = "$name: ${value.trimTo(MAX_NAME_LENGTH)}"
                tooltip = value.trimTo(MAX_TOOLTIP_LENGTH)
            }
        }
    }

    private fun update() {
        if (isEnabled) {
            icon = when {
                !isTrusted -> AllIcons.General.ShowWarning
                !isServiceEnabled || !isChecked -> AllIcons.Actions.Pause
                else -> myIcon
            }
            _controlType = if (isTrusted) {
                ControlType.CHECKBOX
            } else {
                ControlType.CHECKBOX_LOCKED
            }
        }
    }

    private fun String.trimTo(len: Int): String {
        return if (length > len) {
            "${take(len)}..."
        } else {
            this
        }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        update()
    }

    override fun toString(): String {
        return text
    }
}
