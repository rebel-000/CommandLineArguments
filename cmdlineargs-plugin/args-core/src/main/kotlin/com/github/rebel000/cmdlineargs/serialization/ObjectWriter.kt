package com.github.rebel000.cmdlineargs.serialization

internal interface ObjectWriter {
    fun addArray(key: String, capacity: Int): ArrayWriter
    fun addObject(key: String): ObjectWriter
    operator fun set(key: String, value: Boolean)
    operator fun set(key: String, value: Int)
    operator fun set(key: String, value: Float)
    operator fun set(key: String, value: String)
}
