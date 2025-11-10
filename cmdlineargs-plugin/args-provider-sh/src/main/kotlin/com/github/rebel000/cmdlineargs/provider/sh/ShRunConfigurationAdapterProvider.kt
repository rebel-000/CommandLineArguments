package com.github.rebel000.cmdlineargs.provider.sh

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.sh.run.ShRunConfiguration

internal class ShRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is ShRunConfiguration
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return (s.configuration as? ShRunConfiguration)?.let { ShRunConfigurationAdapter(s) }
    }

    class ShRunConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun getArguments(): String {
            val config = settings.configuration as? ShRunConfiguration ?: return ""
            return config.scriptOptions ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings.configuration as? ShRunConfiguration ?: return
            config.scriptOptions = value
        }
    }
}