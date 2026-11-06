package com.github.rebel000.cmdlineargs.execution.multilaunch

import com.github.rebel000.cmdlineargs.extensions.MultiLaunchPluginAdapter
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.multilaunch.MultiLaunchConfiguration
import com.intellij.execution.multilaunch.execution.executables.impl.RunConfigurationExecutableManager

internal class MultiLaunchAdapter : MultiLaunchPluginAdapter {
    override fun isMultiLaunch(config: RunConfiguration): Boolean = config is MultiLaunchConfiguration

    override fun foreachConfigs(config: RunConfiguration, action: (config: RunnerAndConfigurationSettings) -> Unit) {
        if (config is MultiLaunchConfiguration) {
            config
                .descriptors
                .forEach {
                    (it.executable as? RunConfigurationExecutableManager.RunConfigurationExecutable)
                        ?.let { executable -> action(executable.settings) }
                }
        }
    }
}