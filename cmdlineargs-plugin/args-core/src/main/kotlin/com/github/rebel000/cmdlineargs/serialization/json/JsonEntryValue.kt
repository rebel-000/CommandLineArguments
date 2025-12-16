package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.serialization.ArrayReader
import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

internal class JsonEntryValue(val jElement: JsonElement?) : EntryValue {
    override val asBoolean: Boolean? get() {
        return if (jElement is JsonPrimitive && jElement.isBoolean) {
            jElement.asBoolean
        } else {
            null
        }
    }

    override val asInt: Int? get() {
        return if (jElement is JsonPrimitive && jElement.isNumber) {
            jElement.asInt
        } else {
            null
        }
    }

    override val asFloat: Float? get() {
        return if (jElement is JsonPrimitive && jElement.isNumber) {
            jElement.asFloat
        } else {
            null
        }
    }

    override val asString: String? get() {
        return if (jElement is JsonPrimitive && jElement.isString) {
            jElement.asString
        } else {
            null
        }
    }

    override val asObject: ObjectReader? get() {
        return if (jElement?.isJsonObject == true) {
            JsonObjectReader(jElement.asJsonObject)
        } else {
            null
        }
    }

    override val asArray: ArrayReader? get() {
        return if (jElement?.isJsonArray == true) {
            JsonArrayReader(jElement.asJsonArray)
        } else {
            null
        }
    }
}