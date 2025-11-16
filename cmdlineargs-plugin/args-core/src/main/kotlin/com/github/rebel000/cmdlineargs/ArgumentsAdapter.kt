package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.helpers.matchesWildcard
import com.intellij.execution.RunnerAndConfigurationSettings

abstract class ArgumentsAdapter(val settings: RunnerAndConfigurationSettings) {
    companion object {
        fun runConfigurationPredicate(name: String): ((ArgumentNode) -> Boolean) {
            return { it: ArgumentNode ->
                it.filters["runConfiguration"]?.let { f -> f.any{ name.matchesWildcard(it) }} ?: true
            }
        }
    }

    var isEnabled = true
    abstract fun getArguments(): String
    abstract fun setArguments(value: String)
    open fun onStart() = Unit
    open fun onCleanup() = Unit
    open fun predicate(): ((ArgumentNode) -> Boolean) = runConfigurationPredicate(settings.name)
}