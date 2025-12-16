package com.github.rebel000.cmdlineargs.serialization

internal interface EntryValue {
    val asBoolean: Boolean?
    val asInt: Int?
    val asFloat: Float?
    val asString: String?
    val asObject: ObjectReader?
    val asArray: ArrayReader?
}