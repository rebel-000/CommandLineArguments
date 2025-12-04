package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.util.height
import com.intellij.ui.util.width
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.tree.TreeCellEditor
import kotlin.math.max
import kotlin.math.min

internal class ArgumentTreeCellEditor : JTextField(), TreeCellEditor {
    private var offsetX = 0
    private var offsetY = 0
    private var maxWidth = 0
    private var minWidth = 0
    private var myHeight = 0
    var node: ArgumentNode? = null

    init {
        addActionListener {
            stopCellEditing() 
        }
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        val width = min(max(maxWidth - offsetX - x, minWidth), maxWidth)
        super.setBounds(x + offsetX, y + offsetY, width, myHeight)
    }

    override fun processFocusEvent(e: FocusEvent?) {
        super.processFocusEvent(e)
        if (e?.id == FocusEvent.FOCUS_LOST) {
            val cause = e.cause
            if (cause < FocusEvent.Cause.TRAVERSAL || cause > FocusEvent.Cause.TRAVERSAL_BACKWARD) {
                cancelCellEditing()
            }
        }
    }

    override fun getCellEditorValue(): Any? {
        return text
    }

    override fun isCellEditable(anEvent: EventObject?): Boolean {
        if (anEvent is MouseEvent) {
            return anEvent.getClickCount() >= 2
        }
        return true
    }

    override fun shouldSelectCell(anEvent: EventObject?): Boolean {
        return true
    }

    override fun stopCellEditing(): Boolean {
        val listeners: Array<Any?> = listenerList.getListenerList()
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === CellEditorListener::class.java) {
                (listeners[i + 1] as CellEditorListener).editingStopped(ChangeEvent(this))
            }
            i -= 2
        }
        node = null
        return true
    }

    override fun cancelCellEditing() {
        val listeners: Array<Any?> = listenerList.getListenerList()
        var i = listeners.size - 2
        while (i >= 0) {
            if (listeners[i] === CellEditorListener::class.java) {
                (listeners[i + 1] as CellEditorListener).editingCanceled(ChangeEvent(this))
            }
            i -= 2
        }
        node = null
    }

    override fun addCellEditorListener(l: CellEditorListener?) {
        listenerList.add(CellEditorListener::class.java, l)
    }

    override fun removeCellEditorListener(l: CellEditorListener?) {
        listenerList.remove(CellEditorListener::class.java, l)
    }

    override fun getTreeCellEditorComponent(tree: JTree, value: Any, isSelected: Boolean, expanded: Boolean, leaf: Boolean, row: Int): Component {
        if (value is ArgumentNode) {
            if (componentOrientation.isLeftToRight) {
                offsetX = 0
                val renderer = tree.cellRenderer as ArgumentTreeCellRenderer
                val rendererInsets = renderer.insets
                val textRendererBorder = renderer.textRenderer.myBorder?.getBorderInsets(renderer.textRenderer) ?: JBUI.emptyInsets()
                val textRendererInsets = renderer.textRenderer.insets
                val thisInsets = insets
                offsetX = (rendererInsets.left
                        + renderer.textRenderer.ipad.left
                        + textRendererBorder.left
                        + textRendererInsets.left
                        - thisInsets.left
                        - margin.left)

                offsetY = (rendererInsets.top
                        + renderer.textRenderer.ipad.top
//                        + textRendererBorder.top ??
                        + textRendererInsets.top
                        - thisInsets.top
                        - margin.top)

                val control = renderer.getControl(value.controlType)
                if (control != null) {
                    offsetX += control.width
                }

                val icon = value.icon
                if (icon != null) {
                    offsetX += icon.iconWidth + renderer.textRenderer.iconTextGap
                }

                if (!value.isLeaf) {
                    offsetX += getFontMetrics(font).charWidth('[')
                }
            }
            isEnabled = true
            node = value
            text = value.text
        } else {
            isEnabled = false
            node = null
            text = tree.convertValueToText(value, isSelected, expanded, leaf, row, true)
        }
        maxWidth = tree.parent.width - tree.insets.width - 10
        minWidth = (maxWidth * 0.5).toInt()
        myHeight = tree.rowHeight + insets.height + margin.height
        return this
    }
}
