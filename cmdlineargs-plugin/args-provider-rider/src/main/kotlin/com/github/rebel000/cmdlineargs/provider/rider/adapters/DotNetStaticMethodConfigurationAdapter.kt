package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.run.configurations.method.DotNetStaticMethodConfiguration

internal class DotNetStaticMethodConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    override fun isExperimental(): Boolean = false

    override fun getArguments(): String {
        val config = settings?.configuration as? DotNetStaticMethodConfiguration ?: return ""
        return config.parameters.programParameters
    }

    override fun setArguments(value: String) {
        val config = settings?.configuration as? DotNetStaticMethodConfiguration ?: return
        config.parameters.programParameters = value
    }
}
