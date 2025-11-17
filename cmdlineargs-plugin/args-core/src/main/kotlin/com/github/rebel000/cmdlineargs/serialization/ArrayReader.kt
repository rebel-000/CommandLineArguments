package com.github.rebel000.cmdlineargs.serialization

internal interface ArrayReader {
    fun count(): Int
    fun get(index: Int): EntryValue
    operator fun iterator(): Iterator<EntryValue>
}