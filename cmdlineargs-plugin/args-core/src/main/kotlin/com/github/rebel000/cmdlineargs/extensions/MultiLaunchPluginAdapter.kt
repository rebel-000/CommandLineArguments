package com.github.rebel000.cmdlineargs.extensions

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.extensions.ExtensionPointName

interface MultiLaunchPluginAdapter {
    companion object {
        val EP_NAME: ExtensionPointName<MultiLaunchPluginAdapter> = ExtensionPointName.create("com.github.rebel000.cmdlineargs.extensions.multiLaunchProviderExtension")

        fun isMultiLaunch(config: RunConfiguration): Boolean {
            return EP_NAME.extensionList.firstOrNull()?.isMultiLaunch(config) ?: false
        }

        fun foreachMultilaunchConfigs(config: RunConfiguration, action: (it: RunnerAndConfigurationSettings) -> Unit) {
            EP_NAME.extensionList.firstOrNull()?.foreachConfigs(config, action)
        }
    }

    fun isMultiLaunch(config: RunConfiguration): Boolean
    fun foreachConfigs(config: RunConfiguration, action: (config: RunnerAndConfigurationSettings) -> Unit)
}