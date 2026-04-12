package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.intellij.icons.AllIcons
import com.intellij.util.ui.ThreeStateCheckBox
import java.util.*
import javax.swing.Icon
import javax.swing.tree.MutableTreeNode

class ArgumentNode(name: String) : ArgumentContainer(name) {
    private var _filters: MutableMap<String, MutableSet<String>> = mutableMapOf()
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

    val filtersString: String
        get() {
            return _filtersString
                ?: _filters
                    .asSequence()
                    .joinToString(" ") { it.value.joinToString(" ") }
                    .also { _filtersString = it }
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
            return when {
                !isFolder -> null
                isParameter && isSingle -> AllIcons.Actions.GroupByModule
                isParameter -> AllIcons.Actions.GroupByModuleGroup
                isSingle -> AllIcons.Nodes.Module
                else -> AllIcons.Nodes.Folder
            }
        }
        set(_) {}

    fun hasFilters(): Boolean = _filters.any { it.value.isNotEmpty() }

    fun hasFilter(key: String, value: String?): Boolean {
        return if (value != null) {
            _filters[key]?.contains(value) == true
        } else {
            _filters[key]?.isNotEmpty() == true
        }
    }

    fun getFilter(key: String): Set<String> = _filters[key].orEmpty()

    fun addFilter(key: String, value: String) {
        val values = _filters.getOrPut(key) { mutableSetOf() }
        values.add(value)
        _filtersString = null
    }

    fun removeFilter(key: String, value: String) {
        _filters[key]?.remove(value)
        _filtersString = null
    }

    fun setFilter(key: String, values: List<String>) {
        _filters[key] = values.toMutableSet()
        _filtersString = null
    }

    internal fun validateFilters(filters: Set<String>) {
        _filters = _filters.filter { it.key in filters }.toMutableMap()
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
            isFolder && isSingle -> {
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
            (getParent() as? ArgumentNode)?.invalidate(this)
        }
    }

    override fun serialize(obj: ObjectWriter) {
        obj["name"] = text
        if (description.isNotBlank()) { obj["desc"] = description }
        if (isChecked) { obj["checked"] = true }
        if (_filters.isNotEmpty()) {
            obj.addObject("filters").let {
                for ((key, values) in _filters) {
                    val filterValues = it.addArray(key, values.size)
                    for (value in values) {
                        filterValues.add(value)
                    }
                }
            }
        }
        if (isFolder) {
            if (isParameter) { obj["param"] = true }
            if (join) {
                obj["join"] = true
                obj["join.delimiter"] = joinSeparator
                obj["join.prefix"] = joinPrefix
                obj["join.postfix"] = joinPostfix
            }
            if (isExpanded) { obj["expanded"] = true }
            if (isSingle) { obj["singleChoice"] = true }
            obj.addArray("items", childCount).let {
                for (child in innerArguments()) {
                    child.serialize(it.addObject())
                }
            }

        }
    }

    override fun deserialize(obj: ObjectReader, revision: Int, postprocess: (ArgumentContainer) -> Unit): Boolean {
        val nodeName = obj["name"].asString ?: return false
        text = nodeName
        description = obj["desc"].asString ?: ""
        isParameter = obj["param"].asBoolean ?: false
        isSingle = obj["singleChoice"].asBoolean ?: false
        val oFilters = obj["filters"].asObject
        if (oFilters != null) {
            if (revision >= 3) {
                _filters.clear()
                for ((key, value) in oFilters) {
                    value.asArray?.let { items ->
                        items.iterator()
                            .asSequence()
                            .mapNotNull { it.asString?.takeIf { s -> s.isNotBlank() } }
                            .toMutableSet()
                            .let {
                                if (it.isNotEmpty()) {
                                    _filters[key] = it
                                }
                            }
                    }
                }
            } else {
                _filters.clear()
                for ((key, value) in oFilters) {
                    value.asString?.let { value ->
                        value
                            .splitToSequence(';')
                            .mapNotNull {
                                it.trim().ifEmpty { null }
                            }
                            .toMutableSet()
                            .let {
                                if (it.isNotEmpty()) {
                                    _filters[key] = it
                                }
                            }
                    }
                }
            }
        }
        isChecked = obj["checked"].asBoolean ?: false
        isExpanded = obj["expanded"].asBoolean ?: false
        removeAllChildren()
        obj.get("items").asArray?.let { items ->
            isFolder = revision >= 3 || items.count() > 0
            if (isFolder) {
                join = obj["join"].asBoolean ?: false
                joinSeparator = obj["join.delimiter"].asString ?: ","
                joinPrefix = obj["join.prefix"].asString ?: ""
                joinPostfix = obj["join.postfix"].asString ?: ""
                children = Vector(items.count())
                for (it in items) {
                    it.asObject?.let { item ->
                        val child = ArgumentNode("")
                        if (child.deserialize(item, revision, postprocess)) {
                            child.setParent(this)
                            children.insertElementAt(child, children.size)
                        }
                    }
                }
            }
        }
        invalidate(null)
        postprocess(this)
        return true
    }

    override fun setChecked(checked: Boolean) {
        if (isChecked != checked) {
            if (checked || state == ThreeStateCheckBox.State.DONT_CARE) {
                check()
            } else {
                uncheck()
            }
            (getParent() as? ArgumentNode)?.invalidate(this)
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