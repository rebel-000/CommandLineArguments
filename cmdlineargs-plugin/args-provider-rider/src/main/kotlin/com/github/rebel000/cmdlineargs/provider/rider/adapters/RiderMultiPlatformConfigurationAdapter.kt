package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.multiPlatform.RiderMultiPlatformConfiguration

internal class RiderMultiPlatformConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun getArguments(): String {
        val config = settings?.configuration as? RiderMultiPlatformConfiguration ?: return ""
        return config.parameters.programParameters
    }

    override fun setArguments(value: String) {
        val config = settings?.configuration as? RiderMultiPlatformConfiguration ?: return
        config.parameters.programParameters = value
    }
}
