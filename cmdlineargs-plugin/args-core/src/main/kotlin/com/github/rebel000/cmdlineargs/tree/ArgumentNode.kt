package com.github.rebel000.cmdlineargs.tree

import com.github.rebel000.cmdlineargs.helpers.asBooleanOrNull
import com.github.rebel000.cmdlineargs.helpers.asJsonArrayOrNull
import com.github.rebel000.cmdlineargs.helpers.asJsonObjectOrNull
import com.github.rebel000.cmdlineargs.helpers.asStringOrNull
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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

    override fun serialize(): JsonObject {
        val jResult = JsonObject()
        jResult.addProperty("name", name)
        jResult.addProperty("desc", description)
        jResult.addProperty("checked", isChecked)
        val jFilters = JsonObject()
        for ((key, values) in filters) {
            val jFilterValues = JsonArray()
            for (value in values) {
                jFilterValues.add(value)
            }
            jFilters.add(key, jFilterValues)
        }
        jResult.add("filters", jFilters)
        if (isFolder) {
            jResult.addProperty("param", isParameter)
            if (join) {
                jResult.addProperty("join", true)
                jResult.addProperty("join.delimiter", joinSeparator)
                jResult.addProperty("join.prefix", joinPrefix)
                jResult.addProperty("join.postfix", joinPostfix)
            }
            jResult.addProperty("expanded", isExpanded)
            jResult.addProperty("singleChoice", isSingle)
        }
        if (childCount > 0) {
            val jItems = JsonArray(childCount)
            jResult.add("items", jItems)
            for (child in innerArguments()) {
                jItems.add(child.serialize())
            }
        }
        return jResult
    }

    override fun deserialize(json: JsonObject, revision: Int, postprocess: (ArgumentContainer) -> Unit): Boolean {
        val nodeName = json.get("name")?.asStringOrNull
        if (nodeName != null) {
            name = nodeName
            description = json.get("desc")?.asStringOrNull ?: ""
            isParameter = json.get("param")?.asBooleanOrNull == true
            isSingle = json.get("singleChoice")?.asBooleanOrNull == true
            val jFilters = json.get("filters")?.asJsonObjectOrNull
            if (jFilters != null) {
                filters = if (revision >= 2) {
                    jFilters.entrySet().associate { (key, value) ->
                        val values = value.asJsonArrayOrNull?.mapNotNull { 
                            it.asStringOrNull.orEmpty().trim() 
                        }.orEmpty()
                        Pair(key, values)
                    }
                } else {
                    jFilters.entrySet().associate { (key, value) ->
                        val values = value.asStringOrNull?.split(';')?.mapNotNull {
                            it.trim().ifEmpty { null }
                        }.orEmpty()
                        Pair(key, values)
                    }
                }
            }
            join = json.get("join")?.asBooleanOrNull == true
            joinSeparator = json.get("join.delimiter")?.asStringOrNull ?: ","
            joinPrefix = json.get("join.prefix")?.asStringOrNull ?: ""
            joinPostfix = json.get("join.postfix")?.asStringOrNull ?: ""
            isChecked = json.get("checked")?.asBooleanOrNull == true
            isExpanded = json.get("expanded")?.asBooleanOrNull == true
            removeAllChildren()
            val items = json.getAsJsonArray("items")
            if (items != null) {
                isFolder = true
                children = Vector(items.size())
                for (item in items) {
                    if (item.isJsonObject) {
                        val child = ArgumentNode("")
                        if (child.deserialize(item.asJsonObject, revision, postprocess)) {
                            child.setParent(this)
                            children.insertElementAt(child, children.size)
                        }
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