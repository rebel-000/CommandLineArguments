package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.getQualifiedFilterName
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.matchesWildcard
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.projectView.solution

internal abstract class RiderArgumentsAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
    override fun predicate(): ((ArgumentNode) -> Boolean) {
        val settings = settings ?: return { false }
        val configuration = settings.configuration
        val activeConfigurationPlatform = configuration.project.solution.solutionProperties.activeConfigurationPlatform()
        val activeConfiguration = activeConfigurationPlatform?.configuration ?: ""
        val activePlatform = activeConfigurationPlatform?.platform ?: ""
        val qualifiedFilterName = settings.getQualifiedFilterName()
        return {
            val runConfigurationFilters = it.getFilter("runConfiguration")
            val platformFilters = it.getFilter("platform")
            val configurationFilters = it.getFilter("configuration")
            val passRunConfigurationFilter = runConfigurationFilters.isEmpty() || runConfigurationFilters.any{ f -> 
                qualifiedFilterName.matchesWildcard(f) || settings.name.matchesWildcard(f) 
            }
            val passPlatformFilter = platformFilters.isEmpty() || platformFilters.any{ f -> activePlatform.matchesWildcard(f) }
            val passConfigurationFilter = configurationFilters.isEmpty() || configurationFilters.any{ f -> activeConfiguration.matchesWildcard(f) }
            passRunConfigurationFilter && passPlatformFilter && passConfigurationFilter
        }
    }
}

