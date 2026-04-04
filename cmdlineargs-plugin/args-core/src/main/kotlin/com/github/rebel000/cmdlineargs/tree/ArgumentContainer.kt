package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter

open class ArgumentContainer(name: String) : ArgumentTreeNodeBase(name) {
    override val controlType: Companion.ControlType get() = Companion.ControlType.CHECKBOX

    init {
        isEnabled = true
    }

    fun innerArguments(): Sequence<ArgumentNode> {
        return children().asSequence().filterIsInstance<ArgumentNode>()
    }

    internal open fun serialize(obj: ObjectWriter) {
        obj.addArray("items", childCount).let {
            for (child in innerArguments()) {
                child.serialize(it.addObject())
            }
        }
    }

    internal open fun deserialize(obj: ObjectReader, revision: Int, postprocess: (ArgumentContainer) -> Unit = {}): Boolean {
        removeAllChildren()
        obj["items"].asArray?.let { items ->
            for (it in items) {
                it.asObject?.let { item ->
                    val childNode = ArgumentNode("")
                    if (childNode.deserialize(item, revision, postprocess)) {
                        add(childNode)
                    }
                }
            }
            postprocess(this)
        }
        return true
    }

    override fun isLeaf(): Boolean {
        return false
    }

    override fun toString(): String = "[${text}]"
}

