package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.helpers.tryParseJson
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectReader
import com.github.rebel000.cmdlineargs.serialization.json.JsonObjectWriter
import com.github.rebel000.cmdlineargs.tree.*
import com.google.gson.JsonObject
import com.google.gson.Strictness
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
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
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.io.StringWriter
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
        val jsonSerializer = JsonObjectWriter().apply { add($$"$type", JSON_TYPE) }
        val jsonContent = jsonSerializer.addArray($$"$content", context.tree.selectionCount)
        context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
            it.serialize(jsonContent.addObject())
            context.model.remove(it)
        }
        index?.let { context.tree.addSelectionRow(it) }
        if (!jsonSerializer.jObject.isEmpty) {
            CopyPasteManager.getInstance().setContents(MyTransferable(jsonSerializer.jObject))
        }
    }

    override fun isCutEnabled(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) {
            when {
                it.treeIsEditing -> {
                    false
                }
                it.treeSelectedContainers > 0 -> {
                    it.treeSelectedCount == it.treeSelectedContainers
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun isCutVisible(dc: DataContext): Boolean = true

    override fun performCopy(dc: DataContext) = dc.withArgumentDataContext { context ->
        val isArguments = context.treeSelectedContainers > 0
        if (isArguments) {
            val jsonSerializer = JsonObjectWriter().apply { add($$"$type", JSON_TYPE) }
            val jsonContent = jsonSerializer.addArray($$"$content", context.tree.selectionCount)
            context.tree.forEachSelectedNodeNoRecursion<ArgumentContainer> {
                it.serialize(jsonContent.addObject())
            }
            if (!jsonSerializer.jObject.isEmpty) {
                CopyPasteManager.getInstance().setContents(MyTransferable(jsonSerializer.jObject))
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
                it.treeIsEditing -> {
                    false
                }
                it.treeSelectedContainers > 0 -> {
                    it.treeSelectedCount == it.treeSelectedContainers
                }
                it.treeSelectedConfigurations > 0 -> {
                    it.treeSelectedCount == it.treeSelectedConfigurations
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun isCopyVisible(dc: DataContext): Boolean = true

    override fun performPaste(dc: DataContext) = dc.withArgumentDataContext { context ->
        context.tree.stopEditing()
        val copyPaste = CopyPasteManager.getInstance()
        when {
            copyPaste.areDataFlavorsAvailable(MyTransferable.INNER_FLAVOR) -> {
                copyPaste.getContents<JsonObject>(MyTransferable.INNER_FLAVOR)
                    ?.let { jContent -> pasteAsJson(JsonObjectReader(jContent), context.tree, context.model) }
            }
            copyPaste.areDataFlavorsAvailable(MyTransferable.JSON_FLAVOR) -> {
                copyPaste.getContents<String>(MyTransferable.JSON_FLAVOR)
                    ?.let { pasteAsJson(JsonObjectReader.tryParse(it, null), context.tree, context.model) }
            }
            copyPaste.areDataFlavorsAvailable(DataFlavor.stringFlavor) -> {
                copyPaste.getContents<String>(DataFlavor.stringFlavor)?.let { content ->
                    if (!pasteAsJson(JsonObjectReader.tryParse(content, null), context.tree, context.model)) {
                        pasteAsPlainText(content, context.tree, context.model)
                    }
                }
            }
        }
    }

    override fun isPastePossible(dc: DataContext): Boolean {
        return dc.withArgumentDataContext(false) { context -> context.treeSelectedContainers != 0 && context.treeSelectedCount == 1 }
                && CopyPasteManager.getInstance().areDataFlavorsAvailable(MyTransferable.INNER_FLAVOR, MyTransferable.JSON_FLAVOR, DataFlavor.stringFlavor)
    }

    override fun isPasteEnabled(dc: DataContext): Boolean = true

    private fun pasteAsJson(reader: JsonObjectReader?, tree: ArgumentTree, model: ArgumentTreeModel): Boolean {
        if (reader != null && reader.get($$"$type").asString == JSON_TYPE) {
            reader.get($$"$content").asArray?.let { content ->
                var (parent, index) = model.adjustInsertion(tree.selectedNode())
                for (it in content) {
                    it.asObject?.let { item ->
                        val node = ArgumentNode("")
                        if (node.deserialize(item, ArgumentsService.SERIALIZE_REVISION)) {
                            node.isExpanded = true
                            model.insert(node, parent, index++)
                        }
                    }
                }
                parent.isExpanded = true
                parent.traverse<ArgumentTreeNodeBase> {
                    if (it.isExpanded) {
                        tree.expandPath(TreePath(it.path))
                        return@traverse true
                    }
                    return@traverse false
                }
            }
            return true
        }
        return false
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
        return dc.withArgumentDataContext(false) { context ->
            context.treeSelectedContainers > 0 && !context.treeIsEditing 
        }
    }

    open class MyTransferable(val jObject: JsonObject) : Transferable {
        companion object {
            val INNER_FLAVOR = DataFlavor(
                JsonObject::class.java,
                "com.github.rebel000.cmdlineargs-copy"
            )

            val JSON_FLAVOR = DataFlavor("application/json;class=java.lang.String")

            private val flavors = arrayOf(
                INNER_FLAVOR,
                JSON_FLAVOR,
                DataFlavor.stringFlavor
            )
        }

        override fun getTransferDataFlavors(): Array<DataFlavor?>? = flavors.clone()
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavors.any { it.equals(flavor) }

        override fun getTransferData(flavor: DataFlavor): Any {
            return when (flavor) {
                INNER_FLAVOR -> jObject
                JSON_FLAVOR -> jObject.toString()
                DataFlavor.stringFlavor -> toPrettyString()
                else -> throw UnsupportedFlavorException(flavor)
            }
        }

        private fun toPrettyString(): String {
            try {
                return StringWriter().also {
                    Streams.write(jObject, JsonWriter(it).apply {
                        strictness = Strictness.LENIENT
                        setIndent("    ")
                    })
                }.toString()
            } catch (e: IOException) {
                throw AssertionError(e)
            }
        }
    }
}
