package com.github.rebel000.cmdlineargs.helpers

import com.intellij.execution.RunnerAndConfigurationSettings

fun RunnerAndConfigurationSettings.getArgumentsAdapterKey(): String {
    return "${type.id}:${name}"
}

fun RunnerAndConfigurationSettings.getArgumentsAdapterName(): String {
    return "[${type.displayName}] $name"
}