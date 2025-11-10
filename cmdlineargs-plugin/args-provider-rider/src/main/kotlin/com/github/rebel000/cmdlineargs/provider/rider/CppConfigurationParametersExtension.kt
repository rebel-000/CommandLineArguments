package com.github.rebel000.cmdlineargs.provider.rider

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.provider.rider.adapters.CppProjectConfigurationAdapter
import com.intellij.execution.RunManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.rider.cpp.run.configurations.CppConfigurationParametersExtension
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfigurationType
import com.jetbrains.rider.run.configurations.exe.ExeConfigurationParameters

@Suppress("unused")
class CppConfigurationParametersExtension(private val project: Project) : CppConfigurationParametersExtension, Disposable {
    override fun process(parameters: ExeConfigurationParameters) {
        val argsService = ArgumentsService.getInstance(project)
        if (argsService.isEnabled) {
            val runManager = RunManager.getInstance(project)
            val configNameFromEnv = parameters.envs[CppProjectConfigurationAdapter.ENV_NAME]
            if (configNameFromEnv != null) {
                val config = runManager.findConfigurationByTypeAndName(CppProjectConfigurationType.RUN_CONFIG_ID, configNameFromEnv)
                if (config != null) {
                    parameters.programParameters += " ${argsService.getArguments(config)}"
                } else {
                    project.thisLogger().error("[com.github.rebel000.cmdlineargs] Configuration '$configNameFromEnv' not found")
                }
                parameters.envs = parameters.envs.filterKeys { it != CppProjectConfigurationAdapter.ENV_NAME }
            } else {
                val config = runManager.selectedConfiguration
                if (config != null && config.configuration is CppProjectConfiguration) {
                    parameters.programParameters += " ${argsService.getArguments(config)}"
                } else {
                    project.thisLogger().error("[com.github.rebel000.cmdlineargs] Failed to determine run configuration")
                }
            }
        }
    }

    override fun dispose() {}
}
