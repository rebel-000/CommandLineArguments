package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.github.rebel000.cmdlineargs.tree.visitors.TraverseVisitor
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

    internal open fun serialize(obj: ObjectWriter) {
        val oItems = obj.addArray("items", childCount)
        for (child in innerArguments()) {
            child.serialize(oItems.addObject())
        }
    }

    internal open fun deserialize(obj: ObjectReader, revision: Int, postprocess: (ArgumentContainer) -> Unit = {}): Boolean {
        removeAllChildren()
        val oItems = obj.get("items").asArray
        if (oItems != null) {
            for (it in oItems) {
                val item = it.asObject ?: continue
                val childNode = ArgumentNode("")
                if (childNode.deserialize(item, revision, postprocess)) {
                    add(childNode)
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

