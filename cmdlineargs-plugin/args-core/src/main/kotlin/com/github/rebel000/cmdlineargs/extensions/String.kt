package com.github.rebel000.cmdlineargs.extensions

fun String.matchesWildcard(mask: String): Boolean {
    if (this.isEmpty()) return true
    var tPos = 0
    var mPos = 0
    var wildcard = -1
    var match = 0
    while (tPos < this.length && mPos < mask.length) {
        when {
            this[tPos] == mask[mPos] -> {
                tPos++
                mPos++
            }
            mask[mPos] == '*' -> {
                wildcard = mPos
                match = tPos
                mPos++
            }
            wildcard != -1 -> {
                mPos = wildcard + 1
                match++
                tPos = match
            }
            else -> return false
        }
    }
    while (mPos < mask.length && mask[mPos] == '*') mPos++
    return mPos == mask.length
}
