package com.github.rebel000.cmdlineargs.provider.rider

import com.github.rebel000.cmdlineargs.extensions.PlatformExtension
import com.github.rebel000.cmdlineargs.FilterDefinition
import com.github.rebel000.cmdlineargs.provider.rider.resources.RiderMessages
import com.github.rebel000.cmdlineargs.resources.Messages
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rider.projectView.solution

class RiderPlatformExtension : PlatformExtension {
    override fun isRider(): Boolean = true

    override fun getFilters(project: Project): List<FilterDefinition> {
        val items = (RunManager.getInstanceIfCreated(project)?.allConfigurationsList.orEmpty()).map { it.name }
        val configurationsAndPlatformsCollection =
            project.solution.solutionProperties.configurationsAndPlatformsCollection.valueOrEmpty()
        val (platformFilters, configurationFilters) = configurationsAndPlatformsCollection
            .asSequence()
            .map { Pair(it.platform, it.configuration) }
            .unzip()
        return listOf(
            FilterDefinition(
                "runConfiguration",
                Messages.message("properties.runConfigurationFilters"),
                Messages.message("properties.runConfigurationFilters.desc"),
                items.distinct().sorted()
            ),
            FilterDefinition(
                "platform",
                RiderMessages.message("properties.platformFilters"),
                RiderMessages.message("properties.platformFilters.desc"),
                platformFilters.distinct().sorted()
            ),
            FilterDefinition(
                "configuration",
                RiderMessages.message("properties.configurationFilters"),
                RiderMessages.message("properties.configurationFilters.desc"),
                configurationFilters.distinct().sorted()
            )
        )
    }
}
