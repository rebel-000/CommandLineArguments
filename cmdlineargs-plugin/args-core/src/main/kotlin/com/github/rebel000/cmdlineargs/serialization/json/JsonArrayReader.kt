package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.serialization.ArrayReader
import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.google.gson.JsonArray

internal class JsonArrayReader(val jArray: JsonArray = JsonArray()) : ArrayReader {
    override fun count(): Int = jArray.size()
    override fun get(index: Int): EntryValue = JsonEntryValue(jArray[index])
    override fun iterator(): Iterator<EntryValue> {
        return object : Iterator<EntryValue> {
            val it = jArray.iterator()
            override fun next(): EntryValue = JsonEntryValue(it.next())
            override fun hasNext(): Boolean = it.hasNext()
        }
    }
}