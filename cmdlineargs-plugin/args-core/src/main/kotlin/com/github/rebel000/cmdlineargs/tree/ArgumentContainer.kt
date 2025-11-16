package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.tree.visitors.TraverseVisitor
import com.github.rebel000.cmdlineargs.helpers.asJsonArrayOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Enumeration
import javax.swing.tree.TreeNode

open class ArgumentContainer(var name: String) : ArgumentTreeNodeBase() {
    fun innerArguments(): Sequence<ArgumentNode> {
        return children().asSequence().filterIsInstance<ArgumentNode>()
    }

    fun traverse(visitor: TraverseVisitor) {
        if (this is ArgumentNode) {
            if (visitor.onEnter(this)) {
                val stack = ArrayDeque<Pair<ArgumentNode, Enumeration<TreeNode>>>()
                stack.add(Pair(this, children()))
                while (stack.isNotEmpty()) {
                    val e = stack.last().second
                    if (!e.hasMoreElements()) {
                        val n = stack.removeLast().first
                        visitor.onExit(n)
                        continue
                    }
                    while (e.hasMoreElements()) {
                        val node = e.nextElement()
                        if (node is ArgumentNode && visitor.onEnter(node)) {
                            if (node.childCount > 0) {
                                stack.add(Pair(node, node.children()))
                                break
                            }
                            visitor.onExit(node)
                        }
                    }
                }
            }
        } else {
            for (child in innerArguments()) {
                child.traverse(visitor)
            }
        }
    }

    open fun serialize(): JsonObject {
        val result = JsonObject()
        val items = JsonArray(childCount)
        result.add("items", items)
        for (child in innerArguments()) {
            items.add(child.serialize())
        }
        return result
    }

    open fun deserialize(json: JsonObject, revision: Int, postprocess: (ArgumentContainer) -> Unit = {}): Boolean {
        removeAllChildren()
        val items = json.get("items")?.asJsonArrayOrNull
        if (items != null) {
            for (item in items) {
                if (item.isJsonObject) {
                    val childNode = ArgumentNode("")
                    if (childNode.deserialize(item.asJsonObject, revision, postprocess)) {
                        add(childNode)
                    }
                }
            }
            postprocess(this)
            return true
        }
        return false
    }

    override fun isLeaf(): Boolean {
        return false
    }

    override fun toString(): String = "[${name}]"
}

