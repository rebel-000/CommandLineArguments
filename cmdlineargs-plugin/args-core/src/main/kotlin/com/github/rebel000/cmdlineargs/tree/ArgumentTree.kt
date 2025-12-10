package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.CheckboxTree
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.tree.TreePath

internal class ArgumentTree(private val myModel: ArgumentTreeModel) : CheckboxTree(
    cellRenderer = ArgumentTreeCellRenderer(),
    root =  null,
    checkPolicy = CheckPolicy(
        checkChildrenWithCheckedParent = false,
        uncheckChildrenWithUncheckedParent = false,
        checkParentWithCheckedChild = false,
        uncheckParentWithUncheckedChild = false
    )
), CellEditorListener {
    private val myCellEditor = ArgumentTreeCellEditor()

    init {
        cellEditor = myCellEditor
        cellEditor.addCellEditorListener(this)
        dragEnabled = true
        isEditable = true
        model = myModel
        showsRootHandles = true
        myModel.root.traverse<ArgumentTreeNodeBase> {
            if (it.isExpanded) {
                expandPath(TreePath(it.path))
                return@traverse true
            }
            return@traverse false
        }
    }

    inline fun <reified T : ArgumentTreeNodeBase> forEachSelectedNodeNoRecursion(action: (T) -> Unit) {
        val selectionPaths = selectionPaths?.sortedBy { getRowForPath(it) } ?: return
        var lastPath: TreePath? = null
        for (path in selectionPaths) {
            val node = path.lastPathComponent
            if (node is T && lastPath?.isDescendant(path) != true) {
                lastPath = path
                action(node)
            }
        }
    }

    inline fun <reified T : ArgumentTreeNodeBase> selectedNodesNoRecursion(): List<T> {
        val nodes = ArrayList<T>(selectionPaths.size)
        forEachSelectedNodeNoRecursion<T> {
            nodes.add(it)
        }
        nodes.trimToSize()
        return nodes
    }

    fun selectedNode(): ArgumentTreeNodeBase? = selectionPath?.lastPathComponent as? ArgumentTreeNodeBase

    override fun fireTreeCollapsed(path: TreePath) {
        super.fireTreeCollapsed(path)
        (path.lastPathComponent as? ArgumentTreeNodeBase)?.isExpanded = false
    }

    override fun fireTreeExpanded(path: TreePath) {
        super.fireTreeExpanded(path)
        (path.lastPathComponent as? ArgumentTreeNodeBase)?.isExpanded = true
    }

    override fun isPathEditable(path: TreePath?): Boolean {
        return path?.lastPathComponent is ArgumentNode
    }

    override fun getPopupLocation(event: MouseEvent?): Point? {
        return event?.let { Point(event.x, event.y) }
    }

    override fun editingCanceled(e: ChangeEvent?) {
        val node = myCellEditor.node ?: return
        removeIfEmpty(node)
    }

    override fun editingStopped(e: ChangeEvent?) {
        val node = myCellEditor.node ?: return
        node.text = myCellEditor.text
        removeIfEmpty(node)
    }

    private fun removeIfEmpty(node: ArgumentNode) {
        if (node.text.isEmpty()) {
            val select = (node.nextSibling ?: node.previousSibling ?: node.parent) as? ArgumentTreeNodeBase
            myModel.remove(node)
            select?.let { selectionPaths = arrayOf(TreePath(select.path)) }
        }
    }
}