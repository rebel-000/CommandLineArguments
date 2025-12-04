package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfiguration

internal class DotNetExeConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun isExperimental(): Boolean = false

    override fun getArguments(): String {
        val configuration = settings?.configuration as? DotNetExeConfiguration ?: return ""
        return configuration.parameters.programParameters
    }

    override fun setArguments(value: String) {
        val configuration = settings?.configuration as? DotNetExeConfiguration ?: return
        configuration.parameters.programParameters = value
    }
}