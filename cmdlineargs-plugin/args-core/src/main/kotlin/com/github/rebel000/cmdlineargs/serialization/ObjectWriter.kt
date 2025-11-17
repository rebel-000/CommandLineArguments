package com.github.rebel000.cmdlineargs.serialization

internal interface ObjectWriter {
    fun add(key: String, value: Boolean)
    fun add(key: String, value: Int)
    fun add(key: String, value: Float)
    fun add(key: String, value: String)
    fun addArray(key: String, capacity: Int): ArrayWriter
    fun addObject(key: String): ObjectWriter
}
