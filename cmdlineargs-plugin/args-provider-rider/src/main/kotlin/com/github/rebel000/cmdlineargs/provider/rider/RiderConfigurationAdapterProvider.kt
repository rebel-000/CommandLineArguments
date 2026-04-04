package com.github.rebel000.cmdlineargs.provider.rider

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.github.rebel000.cmdlineargs.extensions.ArgumentsAdapterProviderExtension
import com.github.rebel000.cmdlineargs.provider.rider.adapters.CppProjectConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.DotNetExeConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.DotNetProjectConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.DotNetStaticMethodConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.ExeConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.LaunchSettingsConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.RiderMultiPlatformConfigurationAdapter
import com.github.rebel000.cmdlineargs.provider.rider.adapters.UwpConfigurationAdapter
import com.intellij.execution.RunnerAndConfigurationSettings
import com.jetbrains.rider.cpp.run.configurations.CppProjectConfiguration
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfiguration
import com.jetbrains.rider.run.configurations.exe.ExeConfiguration
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsConfiguration
import com.jetbrains.rider.run.configurations.method.DotNetStaticMethodConfiguration
import com.jetbrains.rider.run.configurations.multiPlatform.RiderMultiPlatformConfiguration
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.jetbrains.rider.run.configurations.uwp.UwpConfiguration

internal class RiderConfigurationAdapterProvider : ArgumentsAdapterProviderExtension {
    override fun createAdapter(s: RunnerAndConfigurationSettings, isRunningCurrentFile: Boolean): ArgumentsAdapter? {
        if (isRunningCurrentFile) {
            return null
        }
        return when (s.configuration) {
            is CppProjectConfiguration -> CppProjectConfigurationAdapter(s)
            is UwpConfiguration -> UwpConfigurationAdapter(s)
            is DotNetExeConfiguration -> DotNetExeConfigurationAdapter(s)
            is DotNetProjectConfiguration -> DotNetProjectConfigurationAdapter(s)
            is ExeConfiguration -> ExeConfigurationAdapter(s)
            is LaunchSettingsConfiguration -> LaunchSettingsConfigurationAdapter(s)
            is DotNetStaticMethodConfiguration -> DotNetStaticMethodConfigurationAdapter(s)
            is RiderMultiPlatformConfiguration -> RiderMultiPlatformConfigurationAdapter(s)
            else -> null
        }
    }
}
