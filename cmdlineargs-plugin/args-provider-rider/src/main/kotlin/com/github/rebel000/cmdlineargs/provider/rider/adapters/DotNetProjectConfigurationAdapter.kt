package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration

internal class DotNetProjectConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun getArguments(): String {
        val config = settings?.configuration as? DotNetProjectConfiguration ?: return ""
        return config.parameters.programParameters
    }

    override fun setArguments(value: String) {
        val config = settings?.configuration as? DotNetProjectConfiguration ?: return
        config.parameters.programParameters = value
    }
}
