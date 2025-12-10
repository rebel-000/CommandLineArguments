package com.github.rebel000.cmdlineargs.tree

import com.intellij.ide.dnd.*
import com.intellij.openapi.Disposable
import com.intellij.ui.awt.RelativeRectangle
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.awt.Component
import java.awt.Point
import javax.swing.tree.TreePath

internal class ArgumentTreeDnDSupport(val tree: ArgumentTree) : DnDDropHandler, DnDTargetChecker {
    enum class DropPosition {
        NONE, BELOW, ABOVE, INTO
    }

    data class DragInfo(val sender: Component, val srcIndex: Int, val srcRows: List<Int>)

    fun install(disposable: Disposable) {
        DnDSupport.createBuilder(tree)
            .setTargetChecker(this)
            .setDropHandler(this)
            .setBeanProvider(this::createDragStartBean)
            .setDisposableParent(disposable)
            .install()
    }

    private fun createDragStartBean(info: DnDActionInfo): DnDDragStartBean? {
        if (tree.isEnabled) {
            tree.selectionRows
                ?.filter { tree.getPathForRow(it)?.lastPathComponent is ArgumentNode }
                ?.sorted()
                ?.ifNotEmpty {
                    val srcIndex = tree.getClosestRowForLocation(info.point.x, info.point.y)
                    if (srcIndex != -1) {
                        return DnDDragStartBean(DragInfo(tree, srcIndex, this))
                    }
                }
        }
        return null
    }

    private fun dropPosition(dragInfo: DragInfo, dstIndex: Int, point: Point): DropPosition {
        if (dragInfo.srcIndex < 0 || dstIndex < 0 || tree.rowCount <= dragInfo.srcIndex || tree.rowCount <= dstIndex) {
            return DropPosition.NONE
        }
        val srcNode = tree.getPathForRow(dragInfo.srcIndex).lastPathComponent as? ArgumentContainer
        val dstNode = tree.getPathForRow(dstIndex).lastPathComponent as? ArgumentContainer
        if (dstNode == null || srcNode === dstNode) {
            return DropPosition.NONE
        } else if (dstNode !is ArgumentNode) {
            return DropPosition.INTO
        }
        val cellBounds = tree.getRowBounds(dstIndex)
        val cellAboveOffset = cellBounds.y + cellBounds.height / 2
        val position = when {
            point.y < cellAboveOffset -> DropPosition.ABOVE
            dstNode.isFolder -> DropPosition.INTO
            else -> DropPosition.BELOW
        }
        val parent = if (position == DropPosition.INTO) {
            dstNode
        } else {
            dstNode.parent as? ArgumentContainer ?: return DropPosition.NONE
        }
        for (srcRow in dragInfo.srcRows) {
            if (srcRow > dstIndex) break
            val srcNode = tree.getPathForRow(srcRow).lastPathComponent as? ArgumentNode ?: continue
            if (srcNode.isNodeDescendant(parent) || srcNode === parent) {
                return DropPosition.NONE
            }
        }
        return position
    }

    override fun drop(event: DnDEvent) {
        val dragInfo = event.attachedObject as? DragInfo
        if (dragInfo?.sender !== tree) {
            event.hideHighlighter()
            return
        }
        var dstIndex = tree.getClosestRowForLocation(event.point.x, event.point.y)
        if (dstIndex == -1) {
            dstIndex = tree.getRowCount() - 1
        }
        if (dragInfo.srcIndex == dstIndex) {
            event.hideHighlighter()
            return
        }
        val position = dropPosition(dragInfo, dstIndex, event.point)
        if (position == DropPosition.NONE) {
            return
        }
        val anchor = tree.getPathForRow(dstIndex).lastPathComponent as? ArgumentTreeNodeBase ?: return
        val model = tree.model as? ArgumentTreeModel ?: return
        var (dst, index) = model.adjustInsertion(anchor, position == DropPosition.ABOVE)
        val ignore = dst.path
        val newSelectionPaths = ArrayList<TreePath>(tree.selectionCount)
        tree.forEachSelectedNodeNoRecursion<ArgumentNode> { node ->
            if (!ignore.contains(node)) {
                val src = node.parent
                val expand = tree.isExpanded(TreePath(node.path))
                if (src === dst && dst.getIndex(node) < index) {
                    model.remove(node)
                    model.insert(node, dst, index - 1)
                } else {
                    model.remove(node)
                    model.insert(node, dst, index++)
                }
                val path = TreePath(node.path)
                if (expand) {
                    tree.expandPath(path)
                }
                newSelectionPaths.add(path)
            }
        }
        newSelectionPaths.trimToSize()
        tree.selectionPaths = newSelectionPaths.toTypedArray()
        tree.expandPath(TreePath(dst.path))
    }

    override fun update(event: DnDEvent): Boolean {
        val dragInfo = event.attachedObject as? DragInfo
        if (dragInfo?.sender !== tree) {
            event.setDropPossible(false, "")
            return true
        }
        val dstIndex = tree.getClosestRowForLocation(event.point.x, event.point.y)
        if (dstIndex == -1) {
            event.setDropPossible(false, "")
            return true
        }
        val position = dropPosition(dragInfo, dstIndex, event.point)
        event.isDropPossible = position != DropPosition.NONE
        if (dragInfo.srcIndex == dstIndex) {
            event.hideHighlighter()
            return true
        }
        val cellBounds = tree.getRowBounds(dstIndex)
        when (position) {
            DropPosition.INTO -> {
                event.setHighlighting(RelativeRectangle(tree, cellBounds), 1)
            }
            DropPosition.ABOVE -> {
                cellBounds.y -= -1
                cellBounds.height = 2
                event.setHighlighting(RelativeRectangle(tree, cellBounds), 2)
            }
            DropPosition.BELOW -> {
                cellBounds.y += cellBounds.height
                cellBounds.y -= -1
                cellBounds.height = 2
                event.setHighlighting(RelativeRectangle(tree, cellBounds), 2)
            }
            else -> {
                event.hideHighlighter()
            }
        }
        return true
    }
}