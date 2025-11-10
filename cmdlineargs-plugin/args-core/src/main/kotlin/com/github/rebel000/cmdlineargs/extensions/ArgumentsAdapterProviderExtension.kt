package com.github.rebel000.cmdlineargs.extensions

import com.github.rebel000.cmdlineargs.ArgumentsAdapter
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.extensions.ExtensionPointName

interface ArgumentsAdapterProviderExtension {
    companion object {
        val EP_NAME: ExtensionPointName<ArgumentsAdapterProviderExtension> =
            ExtensionPointName.create<ArgumentsAdapterProviderExtension>("com.github.rebel000.cmdlineargs.extensions.adapterProvider")
    }

    fun isSupported(s: RunnerAndConfigurationSettings): Boolean
    fun createAdapter(s: RunnerAndConfigurationSettings): ArgumentsAdapter?
}