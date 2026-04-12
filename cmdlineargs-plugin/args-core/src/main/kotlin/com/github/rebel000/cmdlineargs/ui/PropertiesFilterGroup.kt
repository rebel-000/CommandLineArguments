package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.FilterDefinition
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.WrapLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.max

internal class PropertiesFilterGroup(private val definition: FilterDefinition, node: ArgumentNode) {
    private var suspended = false
    private var invalidate = false
    private var columnWidth = 0
    val key get() = definition.key
    val title get() = definition.title
    val field = JBTextField()
    val checkboxes: Array<JBCheckBox>
    val checkboxGroup: JPanel

    init {
        val items = node.getFilter(key)
        field.setEmptyState(definition.desc)
        field.text = items.joinToString("; ")
        field.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                invalidate = true
            }
            override fun removeUpdate(e: DocumentEvent?) {
                invalidate = true
            }
            override fun changedUpdate(e: DocumentEvent?) {
                invalidate = true
            }
        })
        field.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) = Unit
            override fun focusLost(e: FocusEvent?) {
                if (invalidate) {
                    onTextChanged()
                }
            }
        })
        field.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
                if (e?.keyChar == ';') {
                    onTextChanged()
                }
            }
            override fun keyPressed(e: KeyEvent?) = Unit
            override fun keyReleased(e: KeyEvent?) = Unit
        })
        checkboxes = Array(definition.values.size) {
            JBCheckBox(definition.values[it]).apply {
                isSelected = items.contains(text)
                addActionListener { onFlagsChanged(this) }
                columnWidth = max(columnWidth, preferredSize.width)
            }
        }
        checkboxGroup = JPanel(WrapLayout(FlowLayout.LEADING, 0, 0))
        for (it in checkboxes) {
            if (columnWidth > 0) {
                it.preferredSize = Dimension(columnWidth, it.preferredSize.height)
            }
            checkboxGroup.add(it)
        }
    }

    fun items(): List<String> = field.text
        .splitToSequence(';')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()

    private fun onTextChanged() {
        if (!suspended) {
            invalidate = false
            suspended = true
            val items = items()
            checkboxes.forEach { it.isSelected = items.contains(it.text) }
            suspended = false
        }
    }

    private fun onFlagsChanged(checkbox: JBCheckBox) {
        if (!suspended) {
            suspended = true
            val item = checkbox.text
            if (!checkbox.isSelected) {
                field.text = items().filter { it.trim() != item }.joinToString("; ")
            } else {
                if (field.text.isNullOrBlank()) {
                    field.text = item
                } else {
                    field.text = field.text.trimEnd(';') + "; $item"
                }
            }
            suspended = false
        }
    }
}