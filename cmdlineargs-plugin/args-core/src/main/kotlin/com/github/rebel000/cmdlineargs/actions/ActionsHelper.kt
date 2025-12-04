package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.ArgumentDataContext
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext

internal fun AnActionEvent.getArgumentsService(): ArgumentsService? {
    return ArgumentsService.getInstanceIfCreated(project)
}

internal fun AnActionEvent.getArgumentDataContext(): ArgumentDataContext? {
    return getData(ArgumentDataContext.KEY)
}

internal fun AnActionEvent.withArgumentDataContext(action: (ArgumentDataContext) -> Unit) {
    return getData(ArgumentDataContext.KEY)?.let(action) ?: Unit
}

internal fun AnActionEvent.withArgumentDataContext(default: Boolean, action: (ArgumentDataContext) -> Boolean): Boolean {
    return getData(ArgumentDataContext.KEY)?.let(action) ?: default
}

internal fun DataContext.getArgumentDataContext(): ArgumentDataContext? {
    return getData(ArgumentDataContext.KEY)
}

internal fun DataContext.withArgumentDataContext(action: (ArgumentDataContext) -> Unit) {
    getData(ArgumentDataContext.KEY)?.let(action)
}

internal fun DataContext.withArgumentDataContext(default: Boolean, action: (ArgumentDataContext) -> Boolean): Boolean {
    return getData(ArgumentDataContext.KEY)?.let(action) ?: default
}
