package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.helpers.*
import com.github.rebel000.cmdlineargs.serialization.ArrayReader
import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.google.gson.JsonElement

internal class JsonEntryValue(val jElement: JsonElement?) : EntryValue {
    override val asBoolean: Boolean? get() = jElement?.asBooleanOrNull
    override val asInt: Int? get() = jElement?.asIntOrNull
    override val asFloat: Float? get() = jElement?.asFloatOrNull
    override val asString: String? get() = jElement?.asStringOrNull
    override val asObject: ObjectReader? get() = jElement?.asJsonObjectOrNull?.let { JsonObjectReader(it) }
    override val asArray: ArrayReader? get() = jElement?.asJsonArrayOrNull?.let { JsonArrayReader(it) }
}