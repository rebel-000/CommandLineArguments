package com.github.rebel000.cmdlineargs.serialization

internal interface ObjectReader {
    fun count(): Int
    operator fun get(key: String): EntryValue
    operator fun iterator(): Iterator<Pair<String, EntryValue>>
}
