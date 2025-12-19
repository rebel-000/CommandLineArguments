package com.github.rebel000.cmdlineargs.listeners

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

internal class MyExecutionListener(private val project: Project) : ExecutionListener {
    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        ArgumentsService.getInstance(project).onProcessStarting(env)
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
        ArgumentsService.getInstance(project).onProcessNotStarted(env)
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        ArgumentsService.getInstance(project).onProcessStarted(env)
    }
}