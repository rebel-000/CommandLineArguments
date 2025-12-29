package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.resources.Messages
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

internal class ArgumentTreeModel() : TreeModel {
    private val listenerList = mutableListOf<TreeModelListener>()
    private val treeRoot = ArgumentTreeNodeBase("treeRoot")
    private var _projectRoot: ArgumentContainer = ArgumentContainer(Messages.message("toolwindow.projectNode"))
    private var _sharedRoot: ArgumentContainer? = null
    val previewRoot = ArgumentTreeNodeBase(Messages.message("toolwindow.previewNode"))

    var projectRoot: ArgumentContainer
        get() = _projectRoot
        set(value) {
            val index = treeRoot.getIndex(projectRoot)
            rawRemove(_projectRoot)
            _projectRoot = value
            rawInsert(_projectRoot, treeRoot, index)
        }

    var sharedRoot: ArgumentContainer?
        get() = _sharedRoot
        set(value) {
            _sharedRoot?.let { rawRemove(it) }
            _sharedRoot = value
            _sharedRoot?.let {
                rawInsert(_sharedRoot!!, treeRoot, 1)
            }
        }

    init {
        treeRoot.add(previewRoot)
        treeRoot.add(projectRoot)
    }

    fun adjustInsertion(anchor: ArgumentTreeNodeBase?, above: Boolean = false): Pair<ArgumentContainer, Int> {
        if (anchor is ArgumentContainer) {
            if (!anchor.isLeaf && !above) {
                return Pair(anchor, anchor.childCount)
            }
            val parent = anchor.parent
            if (parent is ArgumentContainer) {
                return Pair(parent, parent.getIndex(anchor) + if (above) 0 else 1)
            }
        }
        return Pair(projectRoot, projectRoot.childCount)
    }

    fun add(node: ArgumentNode, hint: ArgumentTreeNodeBase?) {
        val (parent, index) = adjustInsertion(hint)
        insert(node, parent, index)
    }

    fun tryInsertAfter(node: ArgumentNode, after: ArgumentContainer?) {
        val parent = after?.parent as? ArgumentContainer
        if (parent != null) {
            insert(node, parent, parent.getIndex(after) + 1)
        } else {
            add(node, after)
        }
    }

    fun insert(node: ArgumentNode, parent: ArgumentContainer, index: Int) {
        parent.insert(node, index)
        fireTreeNodesInserted(parent.path, intArrayOf(index), arrayOf(node))
    }

    fun rawInsert(node: ArgumentTreeNodeBase, parent: ArgumentTreeNodeBase?, index: Int) {
        val parent = parent ?: treeRoot
        parent.insert(node, index)
        fireTreeNodesInserted(parent.path, intArrayOf(index), arrayOf(node))
    }

    fun rawRemove(node: ArgumentTreeNodeBase) {
        val parent = node.parent as? ArgumentTreeNodeBase ?: return
        val index = parent.getIndex(node)
        parent.remove(index)
        fireTreeNodesRemoved(parent.path, intArrayOf(index), arrayOf(node))
    }

    fun remove(node: ArgumentContainer) {
        if (!node.readonly) {
            rawRemove(node)
            return
        }
        for (child in node.innerArguments()) {
            remove(child)
        }
    }

    fun invalidate() {
        invalidate(root, true)
    }

    fun invalidate(node: ArgumentTreeNodeBase, recursive: Boolean) {
        var recursive = recursive
        if (node is ArgumentNode) {
            if (node.isFolder && node.isSingle) {
                var checked = true
                for (child in node.innerArguments()) {
                    if (child.isChecked) {
                        child.isChecked = checked
                        checked = false
                    }
                }
                recursive = true
            }
            else if (!node.isFolder && node.childCount > 0) {
                val parent = node.parent as ArgumentContainer
                var index = parent.getIndex(node) + 1
                val childNodes = node.innerArguments().toList()
                for (child in childNodes) {
                    remove(child)
                    insert(child, parent, index)
                    index++
                }
            }
        }
        if (recursive) {
            fireTreeStructureChanged(node.path)
        } else {
            val parent = node.parent as? ArgumentTreeNodeBase
            if (parent != null) {
                fireTreeNodesChanged(parent.path, intArrayOf(parent.getIndex(node)), arrayOf(node))
            } else if (node === root) {
                fireTreeNodesChanged(node.path, intArrayOf(), emptyArray())
            }
        }
    }

    private fun fireTreeNodesChanged(path: Array<TreeNode>, childIndices: IntArray, children: Array<TreeNode>) {
        if (listenerList.isNotEmpty()) {
            val event = TreeModelEvent(this, path, childIndices, children)
            for (listener in listenerList.asReversed()) {
                listener.treeNodesChanged(event)
            }
        }
    }

    private fun fireTreeNodesInserted(path: Array<TreeNode>, childIndices: IntArray, children: Array<TreeNode>) {
        if (listenerList.isNotEmpty()) {
            val event = TreeModelEvent(this, path, childIndices, children)
            for (listener in listenerList.asReversed()) {
                listener.treeNodesInserted(event)
            }
        }
    }

    private fun fireTreeNodesRemoved(path: Array<TreeNode>, childIndices: IntArray, children: Array<TreeNode>) {
        if (listenerList.isNotEmpty()) {
            val event = TreeModelEvent(this, path, childIndices, children)
            for (listener in listenerList.asReversed()) {
                listener.treeNodesRemoved(event)
            }
        }
    }

    private fun fireTreeStructureChanged(path: Array<TreeNode>) {
        if (listenerList.isNotEmpty()) {
            val event = TreeModelEvent(this, path)
            for (listener in listenerList.asReversed()) {
                listener.treeStructureChanged(event)
            }
        }
    }

    override fun getRoot(): ArgumentTreeNodeBase {
        return treeRoot
    }

    override fun getChild(parent: Any?, index: Int): ArgumentTreeNodeBase? {
        return (parent as? ArgumentTreeNodeBase)?.getChildAt(index) as? ArgumentTreeNodeBase
    }

    override fun getChildCount(parent: Any?): Int {
        return (parent as? ArgumentTreeNodeBase)?.childCount ?: 0
    }

    override fun isLeaf(node: Any?): Boolean {
        return (node as? ArgumentTreeNodeBase)?.isLeaf ?: return true
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any?) {
        val node = path?.lastPathComponent as ArgumentTreeNodeBase
        node.setUserObject(newValue)
        invalidate(node, false)
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        val parent = parent as? ArgumentTreeNodeBase ?: return -1
        val child = child as? ArgumentTreeNodeBase ?: return -1
        return parent.getIndex(child)
    }

    override fun addTreeModelListener(l: TreeModelListener) {
        listenerList.add(l)
    }

    override fun removeTreeModelListener(l: TreeModelListener) {
        listenerList.remove(l)
    }
}