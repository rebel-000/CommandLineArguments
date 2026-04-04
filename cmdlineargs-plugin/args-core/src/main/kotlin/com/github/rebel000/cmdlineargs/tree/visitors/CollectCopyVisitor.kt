package com.github.rebel000.cmdlineargs.tree.visitors

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.TraverseVisitor

class CollectCopyVisitor  : TraverseVisitor<ArgumentNode> {
    private val indent = StringBuilder()
    private val data = StringBuilder()

    override fun onEnter(node: ArgumentNode): Boolean {
        data.append(indent).append(node.text).append('\n')
        indent.append('\t')
        return true
    }

    override fun onExit(node: ArgumentNode) {
        indent.deleteAt(indent.lastIndex)
    }

    override fun toString(): String {
        return data.toString()
    }
}