package com.github.rebel000.cmdlineargs.listeners

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project

internal class MyRunManagerListener(private val project: Project) : RunManagerListener {
    override fun runConfigurationAdded(s: RunnerAndConfigurationSettings) {
        ArgumentsService.getInstance(project).onRunConfigurationAdded(s)
    }

    override fun runConfigurationChanged(s: RunnerAndConfigurationSettings, existingId: String?) {
        ArgumentsService.getInstance(project).onRunConfigurationChanged(s, existingId)
    }

    override fun runConfigurationRemoved(s: RunnerAndConfigurationSettings) {
        ArgumentsService.getInstance(project).onRunConfigurationRemoved(s)
    }

    override fun runConfigurationSelected(s: RunnerAndConfigurationSettings?) {
        ArgumentsService.getInstance(project).onRunConfigurationSelected(s)
    }
}