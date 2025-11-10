package com.github.rebel000.cmdlineargs.tree.visitors

import com.github.rebel000.cmdlineargs.tree.ArgumentNode

interface TraverseVisitor {
    fun onEnter(node: ArgumentNode): Boolean
    fun onExit(node: ArgumentNode)
}

