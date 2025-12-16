package com.github.rebel000.cmdlineargs

import com.intellij.execution.RunnerAndConfigurationSettings

fun RunnerAndConfigurationSettings.getArgumentsAdapterKey(): String {
    return "${type.id}:${name}"
}

fun RunnerAndConfigurationSettings.getArgumentsAdapterFilterKey(): String {
    return "${type.displayName}:${name}"
}

fun RunnerAndConfigurationSettings.getArgumentsAdapterName(): String {
    return "[${type.displayName}] $name"
}

fun String.matchesWildcard(mask: String): Boolean {
    if (this.isEmpty()) return false
    if (mask.isEmpty() || mask == "*") return true
    var tPos = 0
    var mPos = 0
    var wildcard = -1
    var backtrack = -1
    while (tPos < this.length) {
        when {
            mPos < mask.length && (this[tPos] == mask[mPos] || mask[mPos] == '?') -> {
                tPos++
                mPos++
            }
            mPos < mask.length && mask[mPos] == '*' -> {
                wildcard = mPos
                backtrack = tPos
                mPos++
            }
            wildcard != -1 -> {
                mPos = wildcard + 1
                backtrack++
                tPos = backtrack
            }
            else -> return false
        }
    }
    while (mPos < mask.length && mask[mPos] == '*') mPos++
    return mPos == mask.length
}