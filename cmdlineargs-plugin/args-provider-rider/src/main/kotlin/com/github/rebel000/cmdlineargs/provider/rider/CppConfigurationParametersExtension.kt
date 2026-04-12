package com.github.rebel000.cmdlineargs.provider.rider

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.provider.rider.adapters.CppProjectConfigurationAdapter
import com.intellij.execution.RunManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppConfigurationParametersExtension
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.run.configurations.exe.ExeConfigurationParameters

@Suppress("unused")
class CppConfigurationParametersExtension(private val project: Project) : CppConfigurationParametersExtension, Disposable {
    override fun process(parameters: ExeConfigurationParameters) {
        val service = ArgumentsService.getInstance(project)
        if (service.isEnabled) {
            val runManager = RunManager.getInstance(project)
            val configTypeFromEnv = parameters.envs[CppProjectConfigurationAdapter.ENV_TYPE]
            val configNameFromEnv = parameters.envs[CppProjectConfigurationAdapter.ENV_NAME]
            if (configTypeFromEnv != null && configNameFromEnv != null) {
                val config = runManager.findConfigurationByTypeAndName(configTypeFromEnv, configNameFromEnv)
                if (config != null) {
                    val delim = if (parameters.programParameters.isNotEmpty()) " " else ""
                    parameters.programParameters += "${delim}${service.getArguments(config)}"
                } else {
                    project.thisLogger().error("[com.github.rebel000.cmdlineargs] Configuration '$configNameFromEnv' not found")
                }
            } else {
                val config = runManager.selectedConfiguration
                if (config != null && config.configuration is CppProjectConfiguration) {
                    val delim = if (parameters.programParameters.isNotEmpty()) " " else ""
                    parameters.programParameters += "${delim}${service.getArguments(config)}"
                } else {
                    project.thisLogger().error("[com.github.rebel000.cmdlineargs] Failed to determine run configuration")
                }
            }
            if (configTypeFromEnv != null || configNameFromEnv != null) {
                parameters.envs = parameters.envs.filterKeys {
                    it != CppProjectConfigurationAdapter.ENV_TYPE &&
                    it != CppProjectConfigurationAdapter.ENV_NAME
                }
            }
        }
    }

    override fun dispose() {}
}
