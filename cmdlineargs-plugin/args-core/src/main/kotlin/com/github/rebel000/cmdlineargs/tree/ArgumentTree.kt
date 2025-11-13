package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.ArgumentsToolWindow
import com.github.rebel000.cmdlineargs.ui.PropertiesDialog
import com.intellij.ide.dnd.TransferableList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.RowsDnDSupport
import com.intellij.ui.tree.ui.DefaultTreeUI
import com.intellij.util.ui.EditableModel
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.event.*
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

internal class ArgumentTree(private val project: Project) : CheckboxTree(ArgumentTreeCellRenderer(), null, CHECK_POLICY) {
    companion object {
        val CHECK_POLICY = CheckPolicy(false, false, false, false)
        fun getInstance(project: Project?): ArgumentTree? {
            if (project == null || project.isDisposed) {
                return null
            }
            val component = ToolWindowManager.getInstance(project).getToolWindow(ArgumentsToolWindow.TOOLWINDOW_ID)
                ?.contentManagerIfCreated
                ?.selectedContent
                ?.component
            return if (component is ArgumentsToolWindow) component.tree else null
        }
    }

    private var editorActive: Boolean = false
    private var modelListener: TreeModelListener? = null
    private val modelAdapter: TreeModelAdapter = TreeModelAdapter()
    val model: ArgumentTreeModel get() = modelAdapter.model
    val isEditorActive: Boolean get() = editorActive

    init {
        super.model = TreeModelAdapter()
        setUI(object : DefaultTreeUI() {
            override fun startEditing(path: TreePath?, event: MouseEvent?): Boolean {
                editorActive = super.startEditing(path, event)
                return editorActive
            }
        })
        cellEditor = ArgumentTreeCellEditor()
        cellEditor.addCellEditorListener(object : CellEditorListener {
            override fun editingCanceled(e: ChangeEvent?) {
                editorActive = false
                val editor = cellEditor as ArgumentTreeCellEditor
                val node = editor.editingNode ?: return
                if (node.name.isEmpty()) {
                    removeNode(node)
                }
            }
            override fun editingStopped(e: ChangeEvent?) {
                editorActive = false
                val editor = cellEditor as ArgumentTreeCellEditor
                val node = editor.editingNode ?: return
                val value = editor.cellEditorValue as? String ?: return
                if (value.isNotEmpty()) {
                    node.name = value
                } else {
                    removeNode(node)
                }
            }
            fun removeNode(node: ArgumentTreeNodeBase) {
                val select = node.nextSibling ?: node.previousSibling ?: node.parent
                model.remove(node)
                if (select != null) {
                    selectionPaths = arrayOf(TreePath(node.path))
                }
            }
        })
        isEditable = true
        showsRootHandles = true
        transferHandler = object : TransferHandler() {
            override fun createTransferable(component: JComponent): Transferable? {
                val selection = (component as? JTree)?.selectionPaths
                return if (selection?.isNotEmpty() == true) {
                    object : TransferableList<TreePath>(*selection) {
                        override fun toString(path: TreePath): String {
                            return path.lastPathComponent.toString()
                        }
                    }
                }
                else null
            }
            override fun getSourceActions(c: JComponent) = COPY_OR_MOVE
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                if (!isEditing && e?.button == MouseEvent.BUTTON3) {
                    val node = selectedNode()
                    if (node is ArgumentNode) {
                        val bounds = getPathBounds(TreePath(node.path)) ?: return
                        if (e.y >= bounds.y && e.y <= bounds.y + bounds.height) {
                            editNode(node)
                        }
                    }
                }
            }
        })
        addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent?) {
                val node = event?.path?.lastPathComponent ?: return
                if (node is ArgumentTreeNodeBase) {
                    node.isExpanded = true
                }
            }
            override fun treeCollapsed(event: TreeExpansionEvent?) {
                val node = event?.path?.lastPathComponent ?: return
                if (node is ArgumentTreeNodeBase) {
                    node.isExpanded = false
                }
            }
        })
        restoreExpandState(model.root)
    }

    fun editNode(node: ArgumentNode) {
        if (PropertiesDialog(project, node).showAndGet()) {
            if (node.isFolder) {
                if (node.isSingle) {
                    var checked = true
                    for (child in node.innerArguments()) {
                        if (child.isChecked) {
                            setNodeState(child, checked)
                            checked = false
                        }
                    }
                }
            } else {
                if (node.childCount > 0) {
                    val parent = node.parent as ArgumentContainer
                    var index = parent.getIndex(node) + 1
                    for (child in node.innerArguments()) {
                        model.remove(child)
                        model.insert(child, parent, index)
                        index++
                    }
                }
            }
            model.invalidate(node, false)
        }
    }

    fun selectedNode(): ArgumentTreeNodeBase? = getSelectionPaths()?.firstNotNullOf { it.lastPathComponent as? ArgumentTreeNodeBase }

    inline fun <reified T : CheckedTreeNode> selectedNodes(): List<T> {
        val selectionRows = selectionRows ?: return emptyList()
        val selectedNodes = selectionRows.sorted().mapNotNull { getPathForRow(it).lastPathComponent as? T }
        val selectedSet: HashSet<CheckedTreeNode> = selectedNodes.toHashSet()
        return selectedNodes.filter { node ->
            generateSequence(node.parent as? ArgumentTreeNodeBase) { 
                it.parent as? ArgumentTreeNodeBase 
            }.none { it in selectedSet }
        }
    }

    private fun restoreExpandState(node: ArgumentTreeNodeBase) {
        if (node.isExpanded) {
            expandPath(TreePath(node.path))
        }
        for (child in node.children()) {
            if (child is ArgumentTreeNodeBase) restoreExpandState(child)
        }
    }

    override fun addNotify() {
        super.addNotify()
        if (modelListener == null) {
            modelListener = object : TreeModelListener {
                override fun treeNodesChanged(e: TreeModelEvent?) {
                    val children = e?.children
                    if (children != null && children.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            for (node in children) {
                                if (node is ArgumentTreeNodeBase) {
                                    restoreExpandState(node)
                                }
                            }
                        }
                    }
                }
                override fun treeNodesInserted(e: TreeModelEvent?) {
                    val node = e?.path?.lastOrNull() as? ArgumentTreeNodeBase
                    if (node != null) {
                        ApplicationManager.getApplication().invokeLater {
                            restoreExpandState(node)
                        }
                    }
                }
                override fun treeNodesRemoved(e: TreeModelEvent?) = Unit
                override fun treeStructureChanged(e: TreeModelEvent?) {
                    val node = e?.path?.lastOrNull() as? ArgumentTreeNodeBase
                    if (node != null) {
                        ApplicationManager.getApplication().invokeLater {
                            restoreExpandState(node)
                        }
                    }
                }
            }
            model.addTreeModelListener(modelListener!!)
        }
        restoreExpandState(model.root)
    }

    override fun removeNotify() {
        super.removeNotify()
        if (modelListener != null) {
            model.removeTreeModelListener(modelListener!!)
            modelListener = null
        }
    }

    override fun isPathEditable(path: TreePath?): Boolean {
        return path?.lastPathComponent is ArgumentNode
    }

    inner class TreeModelAdapter : TreeModel, EditableModel, RowsDnDSupport.RefinedDropSupport {
        val model: ArgumentTreeModel = ArgumentsService.getInstance(project).model

        override fun getRoot(): ArgumentTreeNodeBase {
            return model.root
        }

        override fun getChild(parent: Any?, index: Int): ArgumentTreeNodeBase? {
            return model.getChild(parent, index)
        }

        override fun getChildCount(parent: Any?): Int {
            return model.getChildCount(parent)
        }

        override fun isLeaf(node: Any?): Boolean {
            return model.isLeaf(node)
        }

        override fun valueForPathChanged(path: TreePath?, newValue: Any?) {
            return model.valueForPathChanged(path, newValue)
        }

        override fun getIndexOfChild(parent: Any?, child: Any?): Int {
            return model.getIndexOfChild(parent, child)
        }

        override fun addTreeModelListener(l: TreeModelListener?) {
            return model.addTreeModelListener(l ?: return)
        }

        override fun removeTreeModelListener(l: TreeModelListener?) {
            return model.removeTreeModelListener(l ?: return)
        }

        override fun addRow() = Unit
        override fun removeRow(index: Int) = Unit
        override fun exchangeRows(oldIndex: Int, newIndex: Int) = Unit
        override fun canExchangeRows(oldIndex: Int, newIndex: Int) = false

        override fun canDrop(oldIndex: Int, newIndex: Int, position: RowsDnDSupport.RefinedDropSupport.Position): Boolean {
            if (oldIndex < 0 || newIndex < 0 || rowCount <= oldIndex || rowCount <= newIndex) {
                return false
            }
            val dst = getPathForRow(newIndex).lastPathComponent as? ArgumentContainer ?: return false
            if (dst.parent == root && position != RowsDnDSupport.RefinedDropSupport.Position.INTO) {
                return false
            }
            val isAbove = position == RowsDnDSupport.RefinedDropSupport.Position.ABOVE
            val isBelow = position == RowsDnDSupport.RefinedDropSupport.Position.BELOW
            val selectionPaths = selectionPaths ?: return false
            return selectionPaths.none {
                val src = it.lastPathComponent
                (src !is ArgumentTreeNodeBase)
                        || (src === dst)
                        || (src.parent === dst.parent) && (src.previousSibling === dst && isBelow) || (src.nextSibling === dst && isAbove)
            }
        }

        override fun isDropInto(component: JComponent, oldIndex: Int, newIndex: Int): Boolean {
            return when (val node = getPathForRow(newIndex).lastPathComponent) {
                is ArgumentNode -> node.isFolder
                is ArgumentContainer -> true
                else -> false
            }
        }

        override fun drop(oldIndex: Int, newIndex: Int, position: RowsDnDSupport.RefinedDropSupport.Position) {
            val anchor = getPathForRow(newIndex).lastPathComponent as? ArgumentTreeNodeBase ?: return
            var (dst, index) = model.adjustInsertion(anchor, position == RowsDnDSupport.RefinedDropSupport.Position.ABOVE)
            val ignore = dst.path
            val selectedNodes = selectedNodes<ArgumentNode>().filter { !ignore.contains(it) }
            selectionPaths = Array(selectedNodes.count()) { 
                val node = selectedNodes[it]
                val src = node.parent
                val expand = isExpanded(TreePath(node.path))
                if (src === dst && dst.getIndex(node) < index) {
                    model.remove(node)
                    model.insert(node, dst, index - 1)
                } else {
                    model.remove(node)
                    model.insert(node, dst, index++)
                }
                val path = TreePath(node.path)
                if (expand) {
                    expandPath(path)
                }
                path
            }
            expandPath(TreePath(dst.path))
        }
    }
}