package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.tree.ArgumentContainer
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.ArgumentTree
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.util.*
import javax.swing.tree.TreePath
import kotlin.math.max

internal class PasteAction : TreeActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val tree = ArgumentTree.getInstance(e.project) ?: return
        if (tree.isEditing) return
        val content = CopyPasteManager.getInstance().getContents<String?>(DataFlavor.stringFlavor) ?: return
        val items = content.split("\n")
        if (items.isEmpty()) return
        val stack = Stack<State>()
        val indentSize = items.firstNotNullOfOrNull {
            when {
                it.startsWith(' ') -> it.takeWhile { ch -> ch == ' ' }.length
                it.startsWith('\t') -> 1
                else -> null
            }
        } ?: 1
        val indentOffset = parseIndent(items[0], indentSize, 0)
        var (parent, index) = tree.model.adjustInsertion(tree.selectedNode())
        tree.expandPath(TreePath(parent.path))
        var node: ArgumentNode? = null
        var depth = 0
        for (arg in items) {
            if (arg.isBlank()) continue
            val indent = parseIndent(arg, indentSize, indentOffset)
            if (indent > depth) {
                if (node != null) {
                    stack.push(State(parent, index, depth))
                    node.isFolder = true
                    tree.expandPath(TreePath(node.path))
                    parent = node
                    index = 0
                }
                depth = indent
            } else while (indent < depth && stack.isNotEmpty()) {
                val state = stack.pop()
                parent = state.parent
                index = state.index
                depth = state.depth
            }
            node = ArgumentNode(arg.trimStart())
            tree.model.insert(node, parent, index++)
        }
        
    }

    private fun parseIndent(str: String, indentSize: Int, indentOffset: Int): Int {
        val ch = str.firstOrNull() ?: return 0
        if (ch != ' ' && ch != '\t') return 0
        return max((str.takeWhile { it == ch }.length / indentSize) - indentOffset, 0)
    }

    data class State(val parent: ArgumentContainer, val index: Int, val depth: Int)
}