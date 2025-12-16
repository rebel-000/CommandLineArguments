package com.github.rebel000.cmdlineargs

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jdom.Element

@Service(Service.Level.PROJECT)
@State(name = "com.github.rebel000.cmdlineargs.project", storages = [Storage("cmdlineargs.xml", roamingType = RoamingType.DISABLED)])
class ArgumentsProjectStorage : SimplePersistentStateComponent<ArgumentsProjectStorage.State>(State()) {
    companion object {
        fun getInstance(project: Project): ArgumentsProjectStorage = project.getService(ArgumentsProjectStorage::class.java)
    }

    class State : BaseState() {
        var enabledConfigs by stringSet()
        var trustedConfigs by stringSet()
        var trustedConfigTypes by stringSet()
        var projectArguments by property(Element("project-arguments")) { it.isEmpty }
    }
}