package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsConfiguration

internal class LaunchSettingsConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun getArguments(): String {
        val config = settings.configuration as? LaunchSettingsConfiguration ?: return ""
        return config.parameters.runtimeArguments
    }

    override fun setArguments(value: String) {
        val config = settings.configuration as? LaunchSettingsConfiguration ?: return
        config.parameters.runtimeArguments = ""
    }
}
