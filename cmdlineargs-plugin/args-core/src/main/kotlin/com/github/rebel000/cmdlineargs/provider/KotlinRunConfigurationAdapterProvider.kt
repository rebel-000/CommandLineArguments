package com.github.rebel000.cmdlineargs.provider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.CommonProgramRunConfigurationParametersAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.kotlin.commonNative.debugger.RunConfigurationWithExecutable
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

internal class KotlinRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is RunConfigurationWithExecutable
            || s.configuration is KotlinRunConfiguration
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when (s.configuration) {
            is RunConfigurationWithExecutable -> CommonProgramRunConfigurationParametersAdapter(s)
            is KotlinRunConfiguration -> CommonProgramRunConfigurationParametersAdapter(s)
            else -> null
        }
    }
}