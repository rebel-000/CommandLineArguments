package com.github.rebel000.cmdlineargs.provider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.CommonProgramRunConfigurationParametersAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.RunnerAndConfigurationSettings

internal class JavaRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is CommonJavaRunConfigurationParameters
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when(s.configuration) {
            is CommonJavaRunConfigurationParameters -> CommonProgramRunConfigurationParametersAdapter(s)
            else -> null
        }
    }
}