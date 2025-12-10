package com.github.rebel000.cmdlineargs.provider.clion

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.provider.CommonProgramRunConfigurationParametersAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.cidr.cpp.execution.CLionRunConfiguration

class CLionRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        val configuration = s.configuration as? CLionRunConfiguration<*, *>
        return when (configuration) {
            is CommonProgramRunConfigurationParameters -> CLionRunConfigurationAdapter(s)
            else -> null
        }
    }

    class CLionRunConfigurationAdapter(s: RunnerAndConfigurationSettings) : CommonProgramRunConfigurationParametersAdapter(s) {
        override fun isExperimental(): Boolean = false
    }
}
