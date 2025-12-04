package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.intellij.icons.AllIcons
import com.intellij.util.ui.ThreeStateCheckBox
import java.util.*
import javax.swing.Icon
import javax.swing.tree.MutableTreeNode

class ArgumentNode(name: String) : ArgumentContainer(name) {
    private var _filtersValue: MutableMap<String, MutableSet<String>> = mutableMapOf()
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

    var filters: MutableMap<String, MutableSet<String>>
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

    override val controlType: Companion.ControlType get() {
        return if ((parent as? ArgumentNode)?.isSingle == true) {
            Companion.ControlType.RADIOBUTTON
        } else {
            Companion.ControlType.CHECKBOX
        }
    }

    override val readonly: Boolean get() = false

    override var icon: Icon?
        get() {
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
        set(_) {}
    
    internal fun addFilter(key: String, value: String) {
        val values = filters.getOrPut(key) { mutableSetOf() }
        values.add(value)
        _filtersString = null
    }

    internal fun removeFilter(key: String, value: String) {
        filters[key]?.remove(value)
        _filtersString = null
    }

    private fun check() {
        when {
            !isFolder || childCount == 0 -> {
                state = ThreeStateCheckBox.State.SELECTED
            }

            !isSingle -> {
                var result: ThreeStateCheckBox.State? = null
                for (child in innerArguments()) {
                    child.check()
                    val childStatus = child.state
                    if (childStatus == ThreeStateCheckBox.State.DONT_CARE || result != childStatus) {
                        if (result != null) {
                            result = ThreeStateCheckBox.State.DONT_CARE
                            break
                        } else {
                            result = childStatus
                        }
                    }
                }
                state = result ?: ThreeStateCheckBox.State.NOT_SELECTED
            }

            state == ThreeStateCheckBox.State.NOT_SELECTED -> {
                val child = getChildAt(0) as ArgumentNode
                child.check()
                state = child.state
            }

            else -> {
                for (child in innerArguments()) {
                    if (child.isChecked) {
                        child.check()
                        state = child.state
                    }
                }
            }
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

    private fun invalidate(sender: ArgumentNode?) {
        var newState: ThreeStateCheckBox.State = ThreeStateCheckBox.State.NOT_SELECTED

        when {
            isSingle -> {
                if (sender != null) {
                    for (child in innerArguments()) {
                        if (child != sender) {
                            child.uncheck()
                        } else {
                            newState = child.state
                        }
                    }
                } else {
                    innerArguments().firstOrNull { it.isChecked }?.state?.let { newState = it }
                }
            }

            isFolder && childCount > 0 -> {
                var result: ThreeStateCheckBox.State? = null
                for (child in innerArguments()) {
                    val childStatus = child.state
                    if (childStatus == ThreeStateCheckBox.State.DONT_CARE || childStatus != result) {
                        if (result != null) {
                            result = ThreeStateCheckBox.State.DONT_CARE
                            break
                        } else {
                            result = childStatus
                        }
                    }
                }
                newState = result ?: ThreeStateCheckBox.State.NOT_SELECTED
            }
            
            isChecked -> {
                newState = ThreeStateCheckBox.State.SELECTED
            }
        }

        if (state != newState) {
            state = newState
            isChecked = state != ThreeStateCheckBox.State.NOT_SELECTED
            val p = getParent() as? ArgumentNode ?: return
            p.invalidate(this)
        }
    }

    override fun serialize(obj: ObjectWriter) {
        obj.add("name", text)
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
            val oItems = obj.addArray("items", childCount)
            for (child in innerArguments()) {
                child.serialize(oItems.addObject())
            }
        }
    }

    override fun deserialize(obj: ObjectReader, revision: Int, postprocess: (ArgumentContainer) -> Unit): Boolean {
        val nodeName = obj.get("name").asString
        if (nodeName != null) {
            text = nodeName
            description = obj.get("desc").asString ?: ""
            isParameter = obj.get("param").asBoolean == true
            isSingle = obj.get("singleChoice").asBoolean == true
            val oFilters = obj.get("filters").asObject
            if (oFilters != null) {
                if (revision >= 3) {
                    filters = mutableMapOf()
                    for ((key, value) in oFilters) {
                        val valueArray = value.asArray ?: continue
                        val filterValues = mutableSetOf<String>()
                        for (v in valueArray) {
                            val stringValue = v.asString ?: continue
                            if (stringValue.isNotBlank()) {
                                filterValues.add(stringValue)
                            }
                        }
                        if (filterValues.isNotEmpty()) {
                            filters[key] = filterValues
                        }
                    }
                } else {
                    filters = mutableMapOf()
                    for ((key, value) in oFilters) {
                        val valueArray = (value.asString ?: continue).split(';').map(String::trim)
                        val filterValues = mutableSetOf<String>()
                        for (v in valueArray) {
                            if (v.isNotBlank()) {
                                filterValues.add(v)
                            }
                        }
                        if (filterValues.isNotEmpty()) {
                            filters[key] = filterValues
                        }
                    }
                }
            }
            isChecked = obj.get("checked").asBoolean == true
            isExpanded = obj.get("expanded").asBoolean == true
            removeAllChildren()
            val oItems = obj.get("items").asArray
            isFolder = if (revision >=3 ) {
                oItems != null
            } else {
                oItems != null && oItems.count() > 0
            }
            if (isFolder) {
                val oItems = oItems!!
                join = obj.get("join").asBoolean == true
                joinSeparator = obj.get("join.delimiter").asString ?: ","
                joinPrefix = obj.get("join.prefix").asString ?: ""
                joinPostfix = obj.get("join.postfix").asString ?: ""
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
            invalidate(null)
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
            p.invalidate(this)
        }
    }

    override fun insert(newChild: MutableTreeNode?, childIndex: Int) {
        super.insert(newChild, childIndex)
        if (newChild is ArgumentNode) {
            if (!isChecked || isSingle && newChild.isChecked) {
                newChild.uncheck()
            }
        }
        invalidate(null)
    }

    override fun setParent(newParent: MutableTreeNode?) {
        (parent as? ArgumentNode)?.invalidate(null)
        super.setParent(newParent)
    }

    override fun isLeaf(): Boolean {
        return !isFolder
    }

    override fun toString(): String {
        if (isFolder) return super.toString()
        return text
    }
}