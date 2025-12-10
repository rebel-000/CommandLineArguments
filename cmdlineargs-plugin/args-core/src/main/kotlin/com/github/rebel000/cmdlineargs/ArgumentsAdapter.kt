package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterFilterKey
import com.github.rebel000.cmdlineargs.helpers.getArgumentsAdapterKey
import com.github.rebel000.cmdlineargs.helpers.matchesWildcard
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

    private val _settings = WeakReference(settings)
    val key = settings.getArgumentsAdapterKey()
    val filterKey = settings.getArgumentsAdapterFilterKey()
    val type = settings.type
    val name = settings.name
    val settings: RunnerAndConfigurationSettings?
        get() = _settings.get()

    var enabled = false
    abstract fun isExperimental(): Boolean
    abstract fun getArguments(): String
    abstract fun setArguments(value: String)
    open fun onStart() = Unit
    open fun onCleanup() = Unit
    open fun predicate(): ((ArgumentNode) -> Boolean) = runConfigurationPredicate(filterKey)

    fun isTrusted(): Boolean {
        if (isExperimental()) {
            val service = ArgumentsService.getInstance(settings?.configuration?.project ?: return false)
            val storage = service.getProjectStorage()
            return storage.trustedConfigTypes.contains(type.id) || storage.trustedConfigs.contains(key)
        }
        return true
    }

    internal fun isTrustedByName(): Boolean? {
        if (isExperimental()) {
            return ArgumentsService
                .getInstance(settings?.configuration?.project ?: return false)
                .getProjectStorage()
                .trustedConfigs
                .contains(key)
        }
        return null
    }

    internal fun isTrustedByType(): Boolean? {
        if (isExperimental()) {
            return ArgumentsService
                .getInstance(settings?.configuration?.project ?: return false)
                .getProjectStorage()
                .trustedConfigTypes
                .contains(type.id)
        }
        return null
    }

    internal fun setTrustedByName(trusted: Boolean) {
        if (isExperimental()) {
            val service = ArgumentsService.getInstance(settings?.configuration?.project ?: return)
            if (trusted) {
                service.getProjectStorage().trustedConfigs.add(key)
            } else {
                service.getProjectStorage().trustedConfigs.remove(key)
            }
        }
    }

    internal fun setTrustedByType(trusted: Boolean) {
        if (isExperimental()) {
            val service = ArgumentsService.getInstance(settings?.configuration?.project ?: return)
            if (trusted) {
                service.getProjectStorage().trustedConfigTypes.add(type.id)
            } else {
                service.getProjectStorage().trustedConfigTypes.remove(type.id)
            }
        }
    }

    internal fun isVisible(showExperimental: Boolean): Boolean {
        return showExperimental || !isExperimental()
    }
}