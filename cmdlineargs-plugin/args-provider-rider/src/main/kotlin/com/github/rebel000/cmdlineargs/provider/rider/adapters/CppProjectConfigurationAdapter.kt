package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.cpp.run.configurations.MutableConfigurationAndPlatformParametersMap
import com.jetbrains.rider.cpp.run.configurations.MutableConfigurationParameters
import com.jetbrains.rider.cpp.run.configurations.launch.LocalCppProjectLaunchParameters
import com.jetbrains.rider.cpp.run.configurations.rdjson.RdJsonLaunchParameters
import com.jetbrains.rider.projectView.solution
import org.jdom.Element

internal class CppProjectConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    companion object {
        const val ENV_TYPE = "__COM_GITHUB_REBEL000_CMDLINEARGS_PROJECT_TYPE"
        const val ENV_NAME = "__COM_GITHUB_REBEL000_CMDLINEARGS_PROJECT_NAME"
    }

    private var cachedArgs = ""

    override fun isExperimental(): Boolean = false

    override fun getArguments(): String {
        return if (enabled) cachedArgs else ""
    }

    override fun setArguments(value: String) {
        cachedArgs = value
    }

    private fun tryGetParametersWithInternalApi(config: CppProjectConfiguration): MutableConfigurationParameters? {
        // fragile hack to pass configuration type and name to the CppConfigurationParametersExtension
        try {
            val parametersMap = config.parameters.parametersMap
            if (parametersMap::class.java.name != MutableConfigurationAndPlatformParametersMap::class.java.name) return null
            val configurationPlatform = config.project.solution.solutionProperties.activeConfigurationPlatform.value ?: return null
            val getParameters = parametersMap::class.java.getDeclaredMethod("getPreSetupParametersForConfigurationAndPlatform", String::class.java, String::class.java)
//            val internalClass = Class.forName("com.jetbrains.rider.cpp.run.configurations.MutableConfigurationAndPlatformParametersMap")
//            val getPreSetupParametersForConfigurationAndPlatform = internalClass.getMethod("getPreSetupParametersForConfigurationAndPlatform", String::class.java, String::class.java)
            return getParameters.invoke(parametersMap, configurationPlatform.configuration, configurationPlatform.platform) as? MutableConfigurationParameters
        } catch (_: Exception) {
            return null
        }
    }

    override fun onStart() {
        val config = settings?.configuration as? CppProjectConfiguration ?: return
        tryGetParametersWithInternalApi(config)?.getCurrentLaunchParameters()?.let { 
            when (it) {
                is LocalCppProjectLaunchParameters -> {
                    it.envs[ENV_TYPE] = config.type.id
                    it.envs[ENV_NAME] = config.name
                    return
                }
                is RdJsonLaunchParameters -> {
                    it.envs[ENV_TYPE] = config.type.id
                    it.envs[ENV_NAME] = config.name
                    return
                }
            }
        }
        // fallback if internal API is not available
        val selectedConfiguration = RunManager
            .getInstanceIfCreated(config.project)
            ?.selectedConfiguration
            ?.configuration
        if (selectedConfiguration == null
            || selectedConfiguration.type != config.type
            || selectedConfiguration.name != config.name
        ) {
            val element = Element("configuration")
            config.parameters.parametersMap.writeExternal(element)
            for (child in element.children) {
                val envs = child.getChild("envs")
                    ?: Element("envs").apply {
                        child.addContent(this)
                    }
                val configTypeEnv = envs.children.firstOrNull { it.getAttributeValue("name") == ENV_TYPE }
                    ?: Element("env").apply {
                        setAttribute("name", ENV_TYPE)
                        envs.addContent(this)
                    }
                val configNameEnv = envs.children.firstOrNull { it.getAttributeValue("name") == ENV_NAME }
                    ?: Element("env").apply {
                        setAttribute("name", ENV_NAME)
                        envs.addContent(this)
                    }
                configTypeEnv.setAttribute("value", config.type.id)
                configNameEnv.setAttribute("value", config.name)
            }
            config.parameters.parametersMap.readExternal(element)
        }
    }

    override fun onCleanup() {
        val config = settings?.configuration as? CppProjectConfiguration ?: return
        tryGetParametersWithInternalApi(config)?.getCurrentLaunchParameters()?.let {
            when (it) {
                is LocalCppProjectLaunchParameters -> {
                    it.envs.remove(ENV_NAME)
                    it.envs.remove(ENV_TYPE)
                    return
                }
                is RdJsonLaunchParameters -> {
                    it.envs.remove(ENV_NAME)
                    it.envs.remove(ENV_TYPE)
                    return
                }
            }
        }
        // fallback if internal API is not available
        var isModified = false
        val element = Element("configuration")
        config.parameters.parametersMap.writeExternal(element)
        for (child in element.children) {
            child.getChild("envs")?.let { envs ->
                isModified = envs.children.removeIf { 
                    val name = it.getAttributeValue("name")
                    name == ENV_TYPE || name == ENV_NAME
                } || isModified
            }
        }
        if (isModified) {
            config.parameters.parametersMap.readExternal(element)
        }
    }
}
