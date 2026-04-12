package com.github.rebel000.cmdlineargs.provider.rider

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.extensions.PlatformExtension
import com.github.rebel000.cmdlineargs.FilterDefinition
import com.github.rebel000.cmdlineargs.getQualifiedFilterName
import com.github.rebel000.cmdlineargs.provider.rider.resources.RiderMessages
import com.github.rebel000.cmdlineargs.resources.Messages
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rider.projectView.solution
import java.util.TreeSet

class RiderPlatformExtension : PlatformExtension {
    override fun isRider(): Boolean = true

    override fun getFilters(project: Project): List<FilterDefinition> {
        val service = ArgumentsService.getInstance(project)
        val rcNameFilters = TreeSet<String>()
        val rcQualifiedNameFilters = TreeSet<String>()
        val platformFilters = TreeSet<String>()
        val configurationFilters = TreeSet<String>()
        RunManager.getInstanceIfCreated(project)
            ?.allSettings
            ?.forEach { s ->
                if (service.getAdapter(s) != null) {
                    rcNameFilters.add(s.name)
                    rcQualifiedNameFilters.add(s.getQualifiedFilterName())
                }
            }
        project.solution
            .solutionProperties
            .configurationsAndPlatformsCollection
            .valueOrEmpty()
            .forEach {
                platformFilters.add(it.platform)
                configurationFilters.add(it.configuration)
            }
        return listOf(
            FilterDefinition(
                "runConfiguration",
                Messages.message("properties.runConfigurationFilters"),
                Messages.message("properties.runConfigurationFilters.desc"),
                rcNameFilters.toList() + rcQualifiedNameFilters.toList()
            ),
            FilterDefinition(
                "platform",
                RiderMessages.message("properties.platformFilters"),
                RiderMessages.message("properties.platformFilters.desc"),
                platformFilters.toList()
            ),
            FilterDefinition(
                "configuration",
                RiderMessages.message("properties.configurationFilters"),
                RiderMessages.message("properties.configurationFilters.desc"),
                configurationFilters.toList()
            )
        )
    }
}
