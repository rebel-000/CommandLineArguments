package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import com.google.gson.JsonObject

internal class JsonObjectReader(val jObject: JsonObject = JsonObject()) : ObjectReader {
    override fun count(): Int = jObject.size()
    override fun get(key: String): EntryValue = JsonEntryValue(jObject.get(key))
    override fun iterator(): Iterator<Pair<String, EntryValue>> {
        return object : Iterator<Pair<String, EntryValue>> {
            val it = jObject.asMap().iterator()
            override fun next(): Pair<String, EntryValue> = it.next().let { (k, v) -> Pair(k, JsonEntryValue(v)) }
            override fun hasNext(): Boolean = it.hasNext()
        }
    }
}