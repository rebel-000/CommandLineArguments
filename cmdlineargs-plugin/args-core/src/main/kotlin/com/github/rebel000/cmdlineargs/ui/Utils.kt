package com.github.rebel000.cmdlineargs.ui

import com.intellij.openapi.actionSystem.DataContext

internal inline fun DataContext.withArgumentDataContext(action: ArgumentDataContext.() -> Unit) {
    getData(ArgumentDataContext.KEY)?.apply(action)
}

internal inline fun <T> DataContext.withArgumentDataContext(default: T, action: ArgumentDataContext.() -> T): T {
    return getData(ArgumentDataContext.KEY)?.let { with(it, action) } ?: default
}