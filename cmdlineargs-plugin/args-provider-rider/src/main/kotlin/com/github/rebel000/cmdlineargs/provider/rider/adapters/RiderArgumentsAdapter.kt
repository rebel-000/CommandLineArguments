package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.helpers.matchesWildcard
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.projectView.solution

internal abstract class RiderArgumentsAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
    override fun predicate(): ((ArgumentNode) -> Boolean) {
        val configuration = settings?.configuration ?: return { false }
        val activeConfigurationPlatform = configuration.project.solution.solutionProperties.activeConfigurationPlatform()
        val activeConfiguration = activeConfigurationPlatform?.configuration ?: ""
        val activePlatform = activeConfigurationPlatform?.platform ?: ""
        return {
            val runConfigurationFilters = it.filters["runConfiguration"].orEmpty()
            val platformFilters = it.filters["platform"].orEmpty()
            val configurationFilters = it.filters["configuration"].orEmpty()
            val passRunConfigurationFilter = runConfigurationFilters.isEmpty() || runConfigurationFilters.any{ f -> name.matchesWildcard(f) }
            val passPlatformFilter = platformFilters.isEmpty() || platformFilters.any{ f -> activePlatform.matchesWildcard(f) }
            val passConfigurationFilter = configurationFilters.isEmpty() || configurationFilters.any{ f -> activeConfiguration.matchesWildcard(f) }
            passRunConfigurationFilter && passPlatformFilter && passConfigurationFilter
        }
    }
}

