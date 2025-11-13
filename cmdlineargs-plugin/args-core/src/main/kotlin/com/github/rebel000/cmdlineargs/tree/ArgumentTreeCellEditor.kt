package com.github.rebel000.cmdlineargs.tree

import java.awt.Component
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.DefaultCellEditor
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.JTree
import kotlin.math.max

internal class ArgumentTreeCellEditor() : DefaultCellEditor(JTextField()) {
    private val textField = editorComponent as JTextField
    var editingNode: ArgumentNode? = null

    private val myEditorComponent = object : JComponent() {
        var editorHeight: Int? = null
        var offsetX: Int = 0
        var offsetY: Int? = null
        var treeWidth: Int = 0

        override fun doLayout() {
            textField.setBounds(offsetX, 0, width - offsetX, height)
        }

        override fun getPreferredSize(): Dimension {
            return textField.preferredSize.let { Dimension(max(it.width + offsetX + 5, (treeWidth * 0.7).toInt()), it.height) }
        }

        override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
            super.setBounds(x, offsetY ?: y, width, editorHeight ?: height)
        }
    }

    init {
        textField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {}
            override fun focusLost(e: FocusEvent?) {
                val cause = e?.cause ?: FocusEvent.Cause.UNKNOWN
                if (cause < FocusEvent.Cause.TRAVERSAL || cause > FocusEvent.Cause.TRAVERSAL_BACKWARD) {
                    cancelCellEditing()
                }
            }
        })
        editorComponent = myEditorComponent
        editorComponent.add(textField)
    }

    override fun getTreeCellEditorComponent(tree: JTree, value: Any, isSelected: Boolean, expanded: Boolean, leaf: Boolean, row: Int): Component {
        if (value is ArgumentNode) {
            if (myEditorComponent.componentOrientation.isLeftToRight) {
                val renderer = tree.cellRenderer as ArgumentTreeCellRenderer
                var offsetX = renderer.insets.left + renderer.textRenderer.ipad.left + renderer.textRenderer.myBorder.getBorderInsets(renderer.textRenderer).left
                offsetX -= textField.insets.left + textField.margin.left
                offsetX += if ((value.parent as? ArgumentNode)?.isSingle == true) {
                    renderer.radioButton.width
                } else {
                    renderer.threeStateCheckBox.width
                }
                if (!value.isLeaf) {
                    offsetX += textField.getFontMetrics(textField.font).charWidth('[')
                }
                val icon = value.icon
                if (icon != null) {
                    offsetX += icon.iconWidth + renderer.textRenderer.iconTextGap
                }
                myEditorComponent.offsetX = offsetX
            }
            delegate.setValue(value.name)
            editingNode = value
        }
        else {
            myEditorComponent.offsetX = 0
            delegate.setValue(tree.convertValueToText(value, isSelected, expanded, leaf, row, false))
        }
        val rowBounds = tree.getRowBounds(row)
        myEditorComponent.offsetY = rowBounds.y - 4
        myEditorComponent.editorHeight = rowBounds.height + 8
        myEditorComponent.treeWidth = tree.width
        return editorComponent
    }
}
