package com.github.rebel000.cmdlineargs.provider.python

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.python.run.PythonRunConfigurationParams
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty

internal class PythonRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is PythonRunConfigurationParams // pycharm run current file
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return (s.configuration as? PythonRunConfigurationParams)?.let { PythonRunConfigurationParamsAdapter(s) }
    }

    class PythonRunConfigurationParamsAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun getArguments(): String {
            val config = settings.configuration as? PythonRunConfigurationParams ?: return ""
            return config.scriptParameters ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings.configuration as? PythonRunConfigurationParams ?: return
            config.scriptParameters = value
        }
    }
}