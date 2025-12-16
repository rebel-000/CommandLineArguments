package com.github.rebel000.cmdlineargs.provider.clion

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.lang.makefile.MakefileRunConfiguration

class MakefileRunConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when (s.configuration) {
            is MakefileRunConfiguration -> MakefileRunConfigurationAdapter(s)
            else -> null
        }
    }

    class MakefileRunConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun isExperimental(): Boolean = false

        override fun getArguments(): String {
            val config = settings?.configuration as? MakefileRunConfiguration ?: return ""
            return config.arguments
        }

        override fun setArguments(value: String) {
            val config = settings?.configuration as? MakefileRunConfiguration ?: return
            config.arguments = value
        }
    }
}
