package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.CheckboxTree
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JTree

internal class ArgumentTreeCellRenderer : CheckboxTree.CheckboxTreeCellRenderer(true, false) {
    val radioButton = JBRadioButton().apply { isOpaque = false }

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
        when (value) {
            is InfoNode -> {
                threeStateCheckBox.isVisible = value.isEnabled
                radioButton.isVisible = false
                textRenderer.icon = value.icon
                textRenderer.append(value.toString(), value.style ?: SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }

            is ArgumentNode -> {
                threeStateCheckBox.isVisible = true
                radioButton.isVisible = true
                val isRadio = radioButton.parent != null
                val useRadio = (value.parent as? ArgumentNode)?.isSingle == true
                if (isRadio != useRadio) {
                    if (useRadio) {
                        add(radioButton, BorderLayout.WEST)
                        remove(threeStateCheckBox)
                    } else {
                        add(threeStateCheckBox, BorderLayout.WEST)
                        remove(radioButton)
                    }
                }
                threeStateCheckBox.state = value.state
                radioButton.isSelected = value.state != ThreeStateCheckBox.State.NOT_SELECTED
                textRenderer.icon = value.icon
                textRenderer.append("$value   ")
                if (value.filters.isNotEmpty()) {
                    textRenderer.append("${value.filtersString} ", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES)
                }
                textRenderer.append(value.description, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
            }

            else -> {
                threeStateCheckBox.isVisible = false
                radioButton.isVisible = false
                textRenderer.icon = null
                textRenderer.append(value.toString())
            }
        }
        textRenderer.foreground = fgColor
    }
}
