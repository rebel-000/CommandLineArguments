package com.github.rebel000.cmdlineargs.provider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.RunnerAndConfigurationSettings

internal class JavaRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when(s.configuration) {
            is CommonJavaRunConfigurationParameters -> JavaRunConfigurationParamsAdapter(s)
            else -> null
        }
    }

    class JavaRunConfigurationParamsAdapter(s: RunnerAndConfigurationSettings) : CommonProgramRunConfigurationParametersAdapter(s) {
        override fun isExperimental(): Boolean = false
    }
}