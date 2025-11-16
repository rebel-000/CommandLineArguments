package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.uwp.UwpConfiguration

internal class UwpConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun getArguments(): String {
        val config = settings?.configuration as? UwpConfiguration ?: return ""
        return config.uwpParameters.programParameters
    }

    override fun setArguments(value: String) {
        val config = settings?.configuration as? UwpConfiguration ?: return
        config.uwpParameters.programParameters = value
    }
}
