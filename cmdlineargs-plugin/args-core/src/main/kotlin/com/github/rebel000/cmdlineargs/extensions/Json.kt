@file:Suppress("unused")

package com.github.rebel000.cmdlineargs.extensions

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.google.gson.JsonPrimitive
import com.intellij.openapi.diagnostic.Logger

fun tryParseJson(json: String, logger: Logger?): JsonElement? {
    return try {
        parseString(json)
    } catch (e: Exception) {
        logger?.error(e)
        null
    }
}

val JsonElement.asJsonArrayOrNull: JsonArray?
    get() = if (isJsonArray) asJsonArray else null

val JsonElement.asJsonObjectOrNull: JsonObject?
    get() = if (isJsonObject) asJsonObject else null

val JsonElement.asStringOrNull: String?
    get() = if (this is JsonPrimitive && isString) asString else null

val JsonElement.asBooleanOrNull: Boolean?
    get() = if (this is JsonPrimitive && isBoolean) asBoolean else null

val JsonElement.asDoubleOrNull: Double?
    get() = if (this is JsonPrimitive && isNumber) asDouble else null

val JsonElement.asFloatOrNull: Float?
    get() = if (this is JsonPrimitive && isNumber) asFloat else null

val JsonElement.asLongOrNull: Long?
    get() = if (this is JsonPrimitive && isNumber) asLong else null

val JsonElement.asShortOrNull: Short?
    get() = if (this is JsonPrimitive && isNumber) asShort else null

val JsonElement.asIntOrNull: Int?
    get() = if (this is JsonPrimitive && isNumber) asInt else null

val JsonElement.asByteOrNull: Byte?
    get() = if (this is JsonPrimitive && isNumber) asByte else null
