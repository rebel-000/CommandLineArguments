package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.ui.ArgumentDataContext
import com.intellij.openapi.actionSystem.AnActionEvent

internal val AnActionEvent?.myService: ArgumentsService? get() {
    return ArgumentsService.getInstanceIfCreated(this?.project)
}

internal inline fun AnActionEvent.withArgumentDataContext(block: ArgumentDataContext.() -> Unit) {
    getData(ArgumentDataContext.KEY)?.apply(block)
}

internal inline fun <T> AnActionEvent.withArgumentDataContext(default: T, block: ArgumentDataContext.() -> T): T {
    return getData(ArgumentDataContext.KEY)?.let { with(it, block) } ?: default
}
