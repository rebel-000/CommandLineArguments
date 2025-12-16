package com.github.rebel000.cmdlineargs.provider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunnerAndConfigurationSettings

abstract class CommonProgramRunConfigurationParametersAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
    override fun getArguments(): String {
        return (settings?.configuration as? CommonProgramRunConfigurationParameters)?.programParameters ?: ""
    }

    override fun setArguments(value: String) {
        (settings?.configuration as? CommonProgramRunConfigurationParameters)?.programParameters = value.ifEmpty { null }
    }
}