package com.github.rebel000.cmdlineargs.provider.rust

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.CommonProgramRunConfigurationParametersAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.RunnerAndConfigurationSettings
import org.rust.cargo.runconfig.RsCommandConfiguration
import org.rust.cargo.runconfig.command.CargoCommandConfiguration

internal class RustArgumentAdapterProviderProvider : ArgumentsAdapterProviderExtension {
    override fun isSupported(s: RunnerAndConfigurationSettings): Boolean {
        return s.configuration is RsCommandConfiguration
            || s.configuration is CargoCommandConfiguration
    }

    override fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter? {
        return when (s.configuration) {
            is CargoCommandConfiguration -> CargoCommandConfigurationAdapter(s)
            is RsCommandConfiguration -> RsCommandConfigurationAdapter(s)
            else -> RsCommandConfigurationAdapter(s)
        }
    }

    class RsCommandConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        init {
            trusted = false
        }

        override fun getArguments(): String {
            val config = settings?.configuration as? RsCommandConfiguration ?: return ""
            return config.programParameters ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings?.configuration as? RsCommandConfiguration ?: return
            val command = config.programParameters?.split(" ")?.firstOrNull() ?: ""
            config.programParameters = "$command $value"
        }
    }

    class CargoCommandConfigurationAdapter(s: RunnerAndConfigurationSettings) : ArgumentsAdapter(s) {
        override fun getArguments(): String {
            val config = settings?.configuration as? CargoCommandConfiguration ?: return ""
            return config.programParameters ?: ""
        }

        override fun setArguments(value: String) {
            val config = settings?.configuration as? CargoCommandConfiguration ?: return
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