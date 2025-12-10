package com.github.rebel000.cmdlineargs.provider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.sh.run.ShRunConfiguration

internal class ShRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when (s.configuration) {
            is ShRunConfiguration -> ShRunConfigurationAdapter(s)
            else -> null
        }
    }

    class ShRunConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun isExperimental(): Boolean = false

        override fun getArguments(): String {
            val config = settings?.configuration as? ShRunConfiguration ?: return ""
            return config.scriptOptions ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings?.configuration as? ShRunConfiguration ?: return
            config.scriptOptions = value
        }
    }
}