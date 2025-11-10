package com.github.rebel000.cmdlineargs.provider.rust

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.RunnerAndConfigurationSettings
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

internal class RustArgumentAdapterProviderProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is CargoCommandConfiguration
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return (s.configuration as? CargoCommandConfiguration)?.let {
            return CargoCommandConfigurationAdapter(s)
        }
    }

    class CargoCommandConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun getArguments(): String {
            val config = settings.configuration as? CargoCommandConfiguration ?: return ""
            return config.programParameters ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings.configuration as? CargoCommandConfiguration ?: return
            val holder = config.parametersHolder ?: return
            val commandArguments = holder.commandArguments.joinToString(" ")
            config.programParameters = if (value.isNotEmpty()) {
                "${holder.command} $commandArguments -- $value"
            } else {
                "${holder.command} $commandArguments"
            }
        }
    }
}