package com.github.rebel000.cmdlineargs.listeners

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MyStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ArgumentsService.getInstance(project).onProjectLoaded()
    }
}