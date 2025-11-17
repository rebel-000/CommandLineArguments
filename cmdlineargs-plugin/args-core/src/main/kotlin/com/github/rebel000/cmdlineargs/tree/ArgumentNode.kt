package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.intellij.icons.AllIcons
import com.intellij.util.ui.ThreeStateCheckBox
import java.util.Vector
import javax.swing.Icon
import javax.swing.tree.MutableTreeNode

class ArgumentNode(name: String) : ArgumentContainer(name) {
    private var _filtersValue: Map<String, List<String>> = emptyMap()
    private var _filtersString: String? = null
    var description: String = ""
    var isFolder: Boolean = false
    var isParameter: Boolean = false
    var isSingle: Boolean = false
    var join: Boolean = false
    var joinSeparator: String = ","
    var joinPrefix: String = "="
    var joinPostfix: String = ""
    var state: ThreeStateCheckBox.State = ThreeStateCheckBox.State.SELECTED; private set

    var filters: Map<String, List<String>>
        get() = _filtersValue
        set(value) {
            _filtersValue = value
            _filtersString = null
        }

    val filtersString: String
        get() {
            if (_filtersString == null) {
                _filtersString = filters.asSequence().joinToString(" ") { it.value.joinToString(" ") }
            }
            return _filtersString!!
        }

    val icon: Icon? get() {
        return if (isFolder) {
            if (isParameter) {
                if (isSingle) AllIcons.Actions.GroupByModule
                else AllIcons.Actions.GroupByModuleGroup
            } else {
                if (isSingle) AllIcons.Nodes.Module
                else AllIcons.Nodes.Folder
            }
        } else null
    }

    private fun check() {
        if (isFolder && childCount > 0) {
            if (isSingle) {
                if (state == ThreeStateCheckBox.State.NOT_SELECTED) {
                    val child = getChildAt(0) as ArgumentNode
                    child.check()
                    state = child.state
                } else {
                    for (child in innerArguments()) {
                        if (child.isChecked) {
                            child.check()
                            state = child.state
                        }
                    }
                }
            } else {
                var result: ThreeStateCheckBox.State? = null
                for (child in innerArguments()) {
                    child.check()
                    val childStatus = child.state
                    if (childStatus == ThreeStateCheckBox.State.DONT_CARE || childStatus != (result
                            ?: childStatus)
                    ) {
                        result = ThreeStateCheckBox.State.DONT_CARE
                        break
                    }
                    result = childStatus
                }
                state = result ?: ThreeStateCheckBox.State.NOT_SELECTED
            }
        } else {
            state = ThreeStateCheckBox.State.SELECTED
        }
        isChecked = state != ThreeStateCheckBox.State.NOT_SELECTED
    }

    private fun uncheck() {
        for (child in innerArguments()) {
            child.uncheck()
        }
        isChecked = false
        state = ThreeStateCheckBox.State.NOT_SELECTED
    }

    private fun update() {
        var newState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.NOT_SELECTED
        if (isSingle) {
            for (child in innerArguments()) {
                if (child.isChecked) {
                    newState = child.state
                    break
                }
            }
        } else if (isFolder && childCount > 0) {
            var result: ThreeStateCheckBox.State? = null
            for (child in innerArguments()) {
                val childStatus = child.state
                if (childStatus == ThreeStateCheckBox.State.DONT_CARE || childStatus != (result ?: childStatus)) {
                    result = ThreeStateCheckBox.State.DONT_CARE
                    break
                }
                result = childStatus
            }
            newState = result ?: ThreeStateCheckBox.State.NOT_SELECTED
        } else if (isChecked) {
            newState = ThreeStateCheckBox.State.SELECTED
        }
        if (state != newState) {
            state = newState
            isChecked = state != ThreeStateCheckBox.State.NOT_SELECTED
            val p = getParent() as? ArgumentNode ?: return
            p.updateSingle(this)
            p.update()
        }
    }

    private fun updateSingle(checkedNode: ArgumentNode) {
        if (isSingle) {
            for (child in innerArguments()) {
                if (child != checkedNode) {
                    child.uncheck()
                }
            }
        }
    }

    override fun serialize(obj: ObjectWriter) {
        obj.add("name", name)
        obj.add("desc", description)
        obj.add("checked", isChecked)
        val oFilters = obj.addObject("filters")
        for ((key, values) in filters) {
            val oFilterValues = oFilters.addArray(key, values.size)
            for (value in values) {
                oFilterValues.add(value)
            }
        }
        if (isFolder) {
            obj.add("param", isParameter)
            if (join) {
                obj.add("join", true)
                obj.add("join.delimiter", joinSeparator)
                obj.add("join.prefix", joinPrefix)
                obj.add("join.postfix", joinPostfix)
            }
            obj.add("expanded", isExpanded)
            obj.add("singleChoice", isSingle)
        }
        if (childCount > 0) {
            val oItems = obj.addArray("items", childCount)
            for (child in innerArguments()) {
                child.serialize(oItems.addObject())
            }
        }
    }

    override fun deserialize(obj: ObjectReader, revision: Int, postprocess: (ArgumentContainer) -> Unit): Boolean {
        val nodeName = obj.get("name").asString
        if (nodeName != null) {
            name = nodeName
            description = obj.get("desc").asString ?: ""
            isParameter = obj.get("param").asBoolean == true
            isSingle = obj.get("singleChoice").asBoolean == true
            val oFilters = obj.get("filters").asObject
            if (oFilters != null) {
                filters = if (revision >= 3) {
                    oFilters.iterator().asSequence().associate { (key, value) ->
                        value.asArray?.let { values ->
                            Pair(
                                key,
                                values.iterator().asSequence().mapNotNull { it.asString?.trim()?.ifEmpty { null } }.toList()
                            )
                        } ?: Pair(key, emptyList())
                    }
                } else {
                    oFilters.iterator().asSequence().associate { (key, value) ->
                        value.asString?.let { values ->
                            Pair(
                                key, 
                                values.split(';').mapNotNull { it.trim().ifEmpty { null } }.toList()
                            )
                        } ?: Pair(key, emptyList())
                    }
                }
            }
            join = obj.get("join").asBoolean == true
            joinSeparator = obj.get("join.delimiter").asString ?: ","
            joinPrefix = obj.get("join.prefix").asString ?: ""
            joinPostfix = obj.get("join.postfix").asString ?: ""
            isChecked = obj.get("checked").asBoolean == true
            isExpanded = obj.get("expanded").asBoolean == true
            removeAllChildren()
            val oItems = obj.get("items").asArray
            if (oItems != null) {
                isFolder = true
                children = Vector(oItems.count())
                for (it in oItems) {
                    val item = it.asObject ?: continue
                    val child = ArgumentNode("")
                    if (child.deserialize(item, revision, postprocess)) {
                        child.setParent(this)
                        children.insertElementAt(child, children.size)
                    }
                }
            }
            update()
            postprocess(this)
            return true
        }
        return false
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            if (checked || state == ThreeStateCheckBox.State.DONT_CARE) {
                check()
            } else {
                uncheck()
            }
            val p = getParent() as? ArgumentNode ?: return
            p.updateSingle(this)
            p.update()
        }
    }

    override fun insert(newChild: MutableTreeNode?, childIndex: Int) {
        super.insert(newChild, childIndex)
        if (newChild is ArgumentNode) {
            if (!isChecked || isSingle && newChild.isChecked) {
                newChild.uncheck()
            }
        }
        update()
    }

    override fun setParent(newParent: MutableTreeNode?) {
        (parent as? ArgumentNode)?.update()
        super.setParent(newParent)
    }

    override fun isLeaf(): Boolean {
        return !isFolder
    }

    override fun toString(): String {
        if (isFolder) return super.toString()
        return name
    }
}