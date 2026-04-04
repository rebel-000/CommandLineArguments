package com.github.rebel000.cmdlineargs.tree

import java.util.Enumeration
import javax.swing.tree.TreeNode
import kotlin.collections.ArrayDeque
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator

interface TraverseVisitor<T> {
    fun onEnter(node: T): Boolean
    fun onExit(node: T)
}

@Suppress("UNCHECKED_CAST")
fun <T : TreeNode> TreeNode.traverse(klass: Class<T>, visitor: TraverseVisitor<T>) {
    if (!klass.isInstance(this)) {
        for (child in children()) {
            if (klass.isInstance(child)) {
                child.traverse(klass, visitor)
            }
        }
    } else if (visitor.onEnter(this as T)) {
        val stack = ArrayDeque<Pair<T, Enumeration<out TreeNode>>>()
        stack.add(this to children())
        while (stack.isNotEmpty()) {
            val e = stack.last().second
            if (!e.hasMoreElements()) {
                val n = stack.removeLast().first
                visitor.onExit(n)
                continue
            }
            while (e.hasMoreElements()) {
                val node = e.nextElement()
                if (klass.isInstance(node) && visitor.onEnter(node as T)) {
                    if (node.childCount > 0) {
                        stack.add(node to node.children())
                        break
                    }
                    visitor.onExit(node)
                }
            }
        }
    }
}

inline fun <reified T : TreeNode> TreeNode.traverse(crossinline action: (T) -> Boolean) {
    traverse<T>(object : TraverseVisitor<T> {
        override fun onEnter(node: T) = action(node)
        override fun onExit(node: T) = Unit
    })
}

inline fun <reified T : TreeNode> TreeNode.traverse(visitor: TraverseVisitor<T>) {
    traverse(T::class.java, visitor)
}
