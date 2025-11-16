package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterKey
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.helpers.matchesWildcard
import com.intellij.execution.RunnerAndConfigurationSettings
import java.lang.ref.WeakReference

abstract class ArgumentsAdapter(settings: RunnerAndConfigurationSettings) {
    companion object {
        fun runConfigurationPredicate(name: String): ((ArgumentNode) -> Boolean) {
            return { it: ArgumentNode ->
                it.filters["runConfiguration"]?.let { f -> f.any{ name.matchesWildcard(it) }} ?: true
            }
        }
    }

    private val _settings = WeakReference(settings)
    val key = settings.getArgumentsAdapterKey()
    val type = settings.type
    val name = settings.name
    val settings: RunnerAndConfigurationSettings?
        get() = _settings.get()

    var enabled = false
    var trusted = true
    abstract fun getArguments(): String
    abstract fun setArguments(value: String)
    open fun onStart() = Unit
    open fun onCleanup() = Unit
    open fun predicate(): ((ArgumentNode) -> Boolean) = runConfigurationPredicate(name)
}