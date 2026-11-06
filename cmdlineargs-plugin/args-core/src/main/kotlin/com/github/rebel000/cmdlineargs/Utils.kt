package com.github.rebel000.cmdlineargs

import com.github.rebel000.cmdlineargs.extensions.MultiLaunchPluginAdapter
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask
import com.intellij.execution.impl.RunManagerImpl
import java.util.*

fun RunnerAndConfigurationSettings.getQualifiedFilterName(): String {
    return "${type.displayName}:$name"
}

fun RunnerAndConfigurationSettings.getQualifiedDisplayName(): String {
    return "[${type.displayName}] $name"
}

fun RunnerAndConfigurationSettings.getEffectiveConfigurations(): Set<String> {
    val toProcess = ArrayDeque<RunnerAndConfigurationSettings>()
    val toSkip = mutableSetOf<RunnerAndConfigurationSettings>()
    val toReturn = mutableSetOf<String>()
    toProcess.add(this)
    while (toProcess.isNotEmpty()) {
        val s = toProcess.pop()
        if (s in toSkip) {
            continue
        }
        toSkip.add(s)
        s.configuration.beforeRunTasks.forEach {
            (it as? RunConfigurableBeforeRunTask)
                ?.settings
                ?.let { s ->
                    toProcess.add(s)
                }
        }
        val config = s.configuration
        when {
            config is CompoundRunConfiguration -> {
                val runManager = RunManager.getInstanceIfCreated(this.configuration.project)
                val runManagerImpl = runManager as? RunManagerImpl
                if (runManagerImpl != null) {
                    config.getConfigurationsWithTargets(runManagerImpl).keys.forEach {
                        runManager.findSettings(it)?.let { s ->
                            toProcess.add(s)
                        }
                    }
                } else {
                    toReturn.add(s.uniqueID)
                }
            }
            MultiLaunchPluginAdapter.isMultiLaunch(config) -> {
                MultiLaunchPluginAdapter.foreachMultilaunchConfigs(config) {
                    toProcess.add(it)
                }
            }
            else -> {
                toReturn.add(s.uniqueID)
            }
        }
    }
    return toReturn
}

fun String.matchesWildcard(mask: String): Boolean {
    if (this.isEmpty()) return false
    if (mask.isEmpty() || mask == "*") return true
    var tPos = 0
    var mPos = 0
    var wildcard = -1
    var backtrack = -1
    while (tPos < this.length) {
        when {
            mPos < mask.length && (this[tPos] == mask[mPos] || mask[mPos] == '?') -> {
                tPos++
                mPos++
            }
            mPos < mask.length && mask[mPos] == '*' -> {
                wildcard = mPos
                backtrack = tPos
                mPos++
            }
            wildcard != -1 -> {
                mPos = wildcard + 1
                backtrack++
                tPos = backtrack
            }
            else -> return false
        }
    }
    while (mPos < mask.length && mask[mPos] == '*') mPos++
    return mPos == mask.length
}