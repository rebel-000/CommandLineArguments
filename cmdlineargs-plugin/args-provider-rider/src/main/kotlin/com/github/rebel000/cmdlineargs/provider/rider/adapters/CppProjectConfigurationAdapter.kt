package com.github.rebel000.cmdlineargs.provider.rider.adapters

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import org.jdom.Element

internal class CppProjectConfigurationAdapter(s: RunnerAndConfigurationSettings) : RiderArgumentsAdapter(s) {
    companion object {
        const val ENV_NAME = "__COM_GITHUB_REBEL000_CMDLINEARGS_CPP_PROJECT"
    }

    private var cachedArgs = ""

    override fun getArguments(): String {
        return if (isEnabled) cachedArgs else ""
    }

    override fun setArguments(value: String) {
        cachedArgs = value
    }

    override fun onStart() {
        val config = settings.configuration as? CppProjectConfiguration ?: return
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
                val configNameEnv = envs.children.firstOrNull { it.getAttributeValue("name") == ENV_NAME }
                    ?: Element("env").apply {
                        setAttribute("name", ENV_NAME)
                        envs.addContent(this)
                    }
                configNameEnv.setAttribute("value", config.name)
            }
            config.parameters.parametersMap.readExternal(element)
        }
    }

    override fun onCleanup() {
        var isModified = false
        val element = Element("configuration")
        val config = settings.configuration as CppProjectConfiguration
        config.parameters.parametersMap.writeExternal(element)
        for (child in element.children) {
            val envs = child.getChild("envs")
            if (envs != null) {
                for (env in envs.children) {
                    if (env.getAttributeValue("name") == ENV_NAME) {
                        envs.removeContent(env)
                        isModified = true
                        break
                    }
                }
            }
        }
        if (isModified) {
            config.parameters.parametersMap.readExternal(element)
        }
    }
}
