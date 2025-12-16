package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.serialization.ArrayWriter
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.google.gson.JsonArray

internal class JsonArrayWriter(val jArray: JsonArray = JsonArray()) : ArrayWriter {
    override fun add(value: Boolean) = jArray.add(value)
    override fun add(value: Int) = jArray.add(value)
    override fun add(value: Float) = jArray.add(value)
    override fun add(value: String) = jArray.add(value)

    override fun addArray(capacity: Int): ArrayWriter {
        val arr = JsonArrayWriter(JsonArray(capacity))
        jArray.add(arr.jArray)
        return arr
    }

    override fun addObject(): ObjectWriter {
        val arr = JsonObjectWriter()
        jArray.add(arr.jObject)
        return arr
    }

    override fun toString(): String = jArray.toString()
}