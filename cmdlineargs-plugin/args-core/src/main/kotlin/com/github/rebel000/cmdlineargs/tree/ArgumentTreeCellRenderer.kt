package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.CheckboxTree
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.AbstractButton
import javax.swing.JTree

internal class ArgumentTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true, false) {
    val radioButton = JBRadioButton().apply { isOpaque = false }

    fun getControl(controlType: ArgumentTreeNodeBase.Companion.ControlType): AbstractButton? {
        return when (controlType) {
            ArgumentTreeNodeBase.Companion.ControlType.CHECKBOX, ArgumentTreeNodeBase.Companion.ControlType.CHECKBOX_LOCKED -> threeStateCheckBox
            ArgumentTreeNodeBase.Companion.ControlType.RADIOBUTTON -> radioButton
            else -> null
        }
    }

    private fun showRadioButton() {
        threeStateCheckBox.isEnabled = true
        threeStateCheckBox.isVisible = true
        radioButton.isVisible = true
        if (radioButton.parent == null) {
            add(radioButton, BorderLayout.WEST)
            remove(threeStateCheckBox)
        }
    }

    private fun showCheckbox(enabled: Boolean) {
        threeStateCheckBox.isEnabled = enabled
        threeStateCheckBox.isVisible = true
        radioButton.isVisible = true
        if (radioButton.parent != null) {
            add(threeStateCheckBox, BorderLayout.WEST)
            remove(radioButton)
        }
    }

    private fun hideControl() {
        threeStateCheckBox.isVisible = false
        radioButton.isVisible = false
    }

    override fun customizeRenderer(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val fgColor: Color = textRenderer.foreground
        if (value is ArgumentTreeNodeBase) {
            textRenderer.icon = value.icon
            when (value.controlType) {
                ArgumentTreeNodeBase.Companion.ControlType.NONE -> hideControl()
                ArgumentTreeNodeBase.Companion.ControlType.CHECKBOX -> showCheckbox(true)
                ArgumentTreeNodeBase.Companion.ControlType.CHECKBOX_LOCKED -> showCheckbox(false)
                ArgumentTreeNodeBase.Companion.ControlType.RADIOBUTTON -> showRadioButton()
            }
            toolTipText = value.tooltip
            val style = value.style ?: SimpleTextAttributes.REGULAR_ATTRIBUTES
            if (value is ConfigurationNode && value.experimental && value.trusted) {
                textRenderer.append("*", style)
            }
            textRenderer.append(value.toString(), style)
            if (value is ConfigurationNode) {
                if (!value.trusted || !value.visible) {
                    threeStateCheckBox.state = ThreeStateCheckBox.State.DONT_CARE
                }
                textRenderer.append(" : ")
                textRenderer.append(value.value, style)
            }
            textRenderer.append("    ")
            if (value is ArgumentNode) {
                threeStateCheckBox.state = value.state
                radioButton.isSelected = value.state != ThreeStateCheckBox.State.NOT_SELECTED
                if (value.hasFilters()) {
                    textRenderer.append("${value.filtersString} ", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES)
                }
                textRenderer.append(value.description, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
            }
        } else {
            hideControl()
            textRenderer.icon = null
            toolTipText = null
            textRenderer.append(value.toString())
        }
        textRenderer.foreground = fgColor
    }
}
