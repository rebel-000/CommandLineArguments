package com.github.rebel000.cmdlineargs.serialization

internal interface ArrayWriter {
    fun add(value: Boolean)
    fun add(value: Int)
    fun add(value: Float)
    fun add(value: String)
    fun addArray(capacity: Int): ArrayWriter
    fun addObject(): ObjectWriter
}