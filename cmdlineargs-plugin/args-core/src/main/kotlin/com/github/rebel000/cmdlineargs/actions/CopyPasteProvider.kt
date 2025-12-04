package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.helpers.tryParseJson
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectReader
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectWriter
import com.github.rebel000.cmdlineargs.tree.*
import com.github.rebel000.cmdlineargs.tree.visitors.CollectCopyVisitor
import com.intellij.ide.CopyProvider
import com.intellij.ide.CutProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.kotlin.idea.refactoring.project
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.tree.TreePath
import kotlin.math.max

internal class CopyPasteProvider : CopyProvider, CutProvider, PasteProvider, DeleteProvider {
    companion object {
        const val JSON_TYPE = "com.github.rebel000.cmdlineargs"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun performCut(dc: DataContext) = dc.withArgumentDataContext { context ->
        val index: Int? = context.tree.selectionRows?.firstOrNull()
        val visitor = CollectCopyVisitor()
        context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
            it.traverse(visitor)
            context.model.remove(it)
        }
        index?.let { context.tree.addSelectionRow(it) }
        val value = visitor.toString()
        if (value.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(value))
        }
    }

    override fun isCutEnabled(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) {
            it.treeSelectedContainers > 0 && it.treeSelectedCount == it.treeSelectedContainers && !it.treeIsEditing
        }
    }

    override fun isCutVisible(dc: DataContext): Boolean = true

    override fun performCopy(dc: DataContext) = dc.withArgumentDataContext { context ->
        val isArguments = context.treeSelectedContainers > 0
        if (isArguments) {
            val jSerializer = JsonObjectWriter()
            jSerializer.add($$"$type", JSON_TYPE)
            val content = jSerializer.addArray($$"$content", context.tree.selectionCount)
            context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
                it.serialize(content.addObject())
            }
            val value = jSerializer.toString()
            if (value.isNotEmpty()) {
                CopyPasteManager.getInstance().setContents(StringSelection(value))
            }
        } else {
            val values = ArrayList<String>()
            val service = ArgumentsService.getInstance(dc.project)
            context.tree.forEachSelectedNodeNoRecursion<ConfigurationNode> { node ->
                node.key?.let { key ->
                    service.findAdapter(key)?.getArguments()?.let { value ->
                        values.add(value)
                    }
                }
            }
            if (values.isNotEmpty()) {
                CopyPasteManager.getInstance().setContents(StringSelection(values.joinToString("\n")))
            }
        }
    }

    override fun isCopyEnabled(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) {
            when {
                it.treeIsEditing -> false
                it.treeSelectedContainers > 0 -> (it.treeSelectedCount == it.treeSelectedContainers)
                it.treeSelectedConfigurations > 0 -> (it.treeSelectedCount == it.treeSelectedConfigurations)
                else -> false
            }
        }
    }

    override fun isCopyVisible(dc: DataContext): Boolean = true

    override fun performPaste(dc: DataContext) = dc.withArgumentDataContext { context ->
        context.tree.stopEditing()
        CopyPasteManager.getInstance().getContents<String?>(DataFlavor.stringFlavor)?.let { content ->
            if (!pasteAsJson(content, context.tree, context.model)) {
                pasteAsPlainText(content, context.tree, context.model)
            }
        }
    }

    override fun isPastePossible(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) {
            it.treeSelectedContainers > 0
        }
    }

    override fun isPasteEnabled(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) { it.treeSelectedCount == 1 } 
                && CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.stringFlavor)
    }

    private fun pasteAsJson(content: String, tree: ArgumentTree, model: ArgumentTreeModel): Boolean {
        val jContent = tryParseJson(content, null)
        if (jContent == null || !jContent.isJsonObject) return false
        val jSerializer = JsonObjectReader(jContent.asJsonObject)
        if (jSerializer.get($$"$type").asString != JSON_TYPE) return false
        val content = jSerializer.get($$"$content").asArray ?: return true
        var (parent, index) = model.adjustInsertion(tree.selectedNode())
        tree.expandPath(TreePath(parent.path))
        for (it in content) {
            val item = it.asObject ?: continue
            val node = ArgumentNode("")
            if (node.deserialize(item, ArgumentsService.SERIALIZE_REVISION)) {
                node.isExpanded = true
                model.insert(node, parent, index++)
                node.traverse<ArgumentNode> {
                    if (node.isExpanded) {
                        tree.expandPath(TreePath(node.path))
                        return@traverse true
                    }
                    return@traverse false
                }
            }
        }
        return true
    }

    private fun pasteAsPlainText(content: String, tree: ArgumentTree, model: ArgumentTreeModel) {
        data class State(val parent: ArgumentContainer, val index: Int, val depth: Int)
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
        var (parent, index) = model.adjustInsertion(tree.selectedNode())
        tree.expandPath(TreePath(parent.path))
        var node: ArgumentNode? = null
        var depth = 0
        for (arg in items) {
            if (arg.isBlank()) continue
            val indent = parseIndent(arg, indentSize, indentOffset)
            if (indent > depth) {
                node?.let { node ->
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
            model.insert(node, parent, index++)
        }
    }

    private fun parseIndent(str: String, indentSize: Int, indentOffset: Int): Int {
        val ch = str.firstOrNull() ?: return 0
        if (ch != ' ' && ch != '\t') return 0
        return max((str.takeWhile { it == ch }.length / indentSize) - indentOffset, 0)
    }

    override fun deleteElement(dc: DataContext) = dc.withArgumentDataContext { context ->
        val index: Int? = context.tree.selectionRows?.firstOrNull()
        context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
            context.model.remove(it)
        }
        index?.let { context.tree.addSelectionRow(it) }
    }

    override fun canDeleteElement(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) {
            it.treeSelectedContainers > 0 && !it.treeIsEditing 
        }
    }
}
