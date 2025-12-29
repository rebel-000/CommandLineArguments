package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.intellij.execution.RunnerAndConfigurationSettings
import java.lang.ref.WeakReference

abstract class ArgumentsAdapter(settings: RunnerAndConfigurationSettings) {
    companion object {
        fun runConfigurationPredicate(name: String): ((ArgumentNode) -> Boolean) {
            return { node: ArgumentNode ->
                node.filters["runConfiguration"]
                    .orEmpty()
                    .let { filter -> filter.isEmpty() || filter.any{ name.matchesWildcard(it) } }
            }
        }
    }

    private var _key = settings.uniqueID
    private var _name = settings.name
    private val _settings = WeakReference(settings)
    private val projectStorage: ArgumentsProjectStorage.State?
        get() = settings?.let { ArgumentsProjectStorage.getInstance(it.configuration.project).state }

    val key: String get() = _key
    val type = settings.type
    val name: String get() = _name
    val settings: RunnerAndConfigurationSettings?
        get() = _settings.get()

    var enabled = false
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
            return projectStorage?.let {
                it.trustedConfigTypes.contains(type.id) || it.trustedConfigs.contains(key)
            } == true
        }
        return true
    }
    
    internal fun invalidate() {
        settings?.let {
            enabled = projectStorage?.enabledConfigs?.contains(it.uniqueID) == true
            _key = it.uniqueID
            _name = it.name
        }
    }

    internal fun isTrustedByName(): Boolean? {
        if (isExperimental()) {
            return projectStorage?.trustedConfigs?.contains(key) == true
        }
        return null
    }

    internal fun isTrustedByType(): Boolean? {
        if (isExperimental()) {
            return projectStorage?.trustedConfigTypes?.contains(type.id) == true
        }
        return null
    }

    internal fun setTrustedByName(trusted: Boolean) {
        if (isExperimental()) {
            if (trusted) {
                projectStorage?.trustedConfigs?.add(key)
            } else {
                projectStorage?.trustedConfigs?.remove(key)
            }
        }
    }

    internal fun setTrustedByType(trusted: Boolean) {
        if (isExperimental()) {
            if (trusted) {
                projectStorage?.trustedConfigTypes?.add(type.id)
            } else {
                projectStorage?.trustedConfigTypes?.remove(type.id)
            }
        }
    }

    internal fun isVisible(showExperimental: Boolean): Boolean {
        return showExperimental || !isExperimental() || isTrusted()
    }
}