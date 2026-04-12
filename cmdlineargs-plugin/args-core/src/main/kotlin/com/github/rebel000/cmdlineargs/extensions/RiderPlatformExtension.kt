package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.FilterDefinition
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface RiderPlatformExtension {
    companion object {
        val EP_NAME: ExtensionPointName<RiderPlatformExtension> = ExtensionPointName.create("com.github.rebel000.cmdlineargs.extensions.riderPlatformExtension")
    }

    fun getFilters(project: Project): List<FilterDefinition>?
    fun setupConfigurationCallback(project: Project, service: ArgumentsService, callback: () -> Unit)
}

