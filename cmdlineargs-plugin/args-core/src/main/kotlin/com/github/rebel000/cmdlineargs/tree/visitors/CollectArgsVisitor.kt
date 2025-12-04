package com.github.rebel000.cmdlineargs.tree.visitors

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.TraverseVisitor

open class CollectArgsVisitor(private val predicate: (ArgumentNode) -> Boolean) : TraverseVisitor<ArgumentNode> {
    private val data = StringBuilder()
    private var prefix: String = ""
    private var separator: String = " "

    override fun onEnter(node: ArgumentNode): Boolean {
        if (node.isChecked && predicate(node)) {
            if (node.isFolder) {
                if (node.isParameter) {
                    data.append(prefix)
                    data.append(node.text)
                    prefix = ""
                }
                if (node.join) {
                    data.append(prefix)
                    data.append(node.joinPrefix)
                    separator = node.joinSeparator
                    prefix = ""
                } else {
                    separator = " "
                    if (node.isParameter) {
                        prefix = " "
                    }
                }
                return true
            } else {
                data.append(prefix)
                data.append(node.text)
                prefix = separator
            }
        }
        return false
    }

    override fun onExit(node: ArgumentNode) {
        if (node.join) {
            data.append(node.joinPostfix)
        }
        val parent = node.parent
        separator = if (parent is ArgumentNode && parent.join) parent.joinSeparator else " "
        prefix = separator
    }

    override fun toString(): String {
        return data.toString()
    }
}