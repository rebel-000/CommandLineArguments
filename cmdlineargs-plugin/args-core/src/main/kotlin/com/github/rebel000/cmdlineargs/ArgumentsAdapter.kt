package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.execution.RunnerAndConfigurationSettings
import java.lang.ref.WeakReference

abstract class ArgumentsAdapter(settings: RunnerAndConfigurationSettings) {
    companion object {
        fun runConfigurationPredicate(name: String): ((ArgumentNode) -> Boolean) {
            return { node: ArgumentNode ->
                node.getFilter("runConfiguration")
                    .let { filter -> filter.isEmpty() || filter.any{ name.matchesWildcard(it) } }
            }
        }
    }

    private var _settings = WeakReference(settings)
    private var _key = settings.uniqueID
    private var _name = settings.name
    private var _trusted = false

    var enabled: Boolean = false
    val key: String get() = _key
    val type = settings.type
    val name: String get() = _name
    val settings: RunnerAndConfigurationSettings? get() = _settings.get()
    val service: ArgumentsService? get() = settings?.configuration?.project?.let { ArgumentsService.getInstance(it) }

    abstract fun isExperimental(): Boolean
    abstract fun getArguments(): String
    abstract fun setArguments(value: String)
    open fun onStart() = Unit
    open fun onCleanup() = Unit

    open fun predicate(): ((ArgumentNode) -> Boolean) {
        return settings
            ?.let { runConfigurationPredicate(it.getQualifiedFilterName()) }
            ?: { false }
    }

    fun isTrusted(): Boolean {
        if (isExperimental()) {
            return isTrustedByName() == true || isTrustedByType() == true
        }
        return true
    }
    
    internal fun invalidate(s: RunnerAndConfigurationSettings) {
        s.let {
            _key = it.uniqueID
            _name = it.name
        }
        _settings = WeakReference(s)
    }

    internal fun isTrustedByName(): Boolean? {
        if (isExperimental()) {
            return _trusted
        }
        return null
    }

    internal fun isTrustedByType(): Boolean? {
        if (isExperimental()) {
            return service?.isTypeTrusted(type.id)
        }
        return null
    }

    internal fun setTrustedByName(trusted: Boolean) {
        if (isExperimental()) {
            _trusted = trusted
        }
    }

    internal fun setTrustedByType(trusted: Boolean) {
        if (isExperimental()) {
            service?.setTypeTrusted(type.id, trusted)
        }
    }

    internal fun fireOnStart() {
        if (enabled && isTrusted()) {
            onStart()
        }
    }

    internal fun fireOnCleanup() {
        if (enabled && isTrusted()) {
            onCleanup()
        }
    }
}