package com.github.rebel000.cmdlineargs.serialization.json

import com.github.rebel000.cmdlineargs.serialization.ArrayWriter
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal class JsonObjectWriter(val jObject: JsonObject = JsonObject()) : ObjectWriter {
    override fun set(key: String, value: Boolean) = jObject.addProperty(key, value)
    override fun set(key: String, value: Int) = jObject.addProperty(key, value)
    override fun set(key: String, value: Float) = jObject.addProperty(key, value)
    override fun set(key: String, value: String) = jObject.addProperty(key, value)

    override fun addArray(key: String, capacity: Int): ArrayWriter {
        val arr = JsonArrayWriter(JsonArray(capacity))
        jObject.add(key, arr.jArray)
        return arr
    }

    override fun addObject(key: String): ObjectWriter {
        val arr = JsonObjectWriter()
        jObject.add(key, arr.jObject)
        return arr
    }

    override fun toString(): String = jObject.toString()
}