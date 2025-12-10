package com.github.rebel000.cmdlineargs.provider.python

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.python.run.PythonRunConfigurationParams

internal class PythonRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when (s.configuration) {
            is PythonRunConfigurationParams -> PythonRunConfigurationParamsAdapter(s)
            else -> null
        }
    }

    class PythonRunConfigurationParamsAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun isExperimental(): Boolean = false

        override fun getArguments(): String {
            val config = settings?.configuration as? PythonRunConfigurationParams ?: return ""
            return config.scriptParameters ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings?.configuration as? PythonRunConfigurationParams ?: return
            config.scriptParameters = value
        }
    }
}