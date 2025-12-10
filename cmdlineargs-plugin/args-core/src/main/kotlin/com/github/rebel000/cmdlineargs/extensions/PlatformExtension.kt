package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.FilterDefinition
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface PlatformExtension {
    companion object {
        val EP_NAME: ExtensionPointName<PlatformExtension> = ExtensionPointName.create("com.github.rebel000.cmdlineargs.extensions.platformExtension")
    }

    fun isRider(): Boolean = false
    fun getFilters(project: Project): List<FilterDefinition>?

}

