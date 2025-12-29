package com.github.rebel000.cmdlineargs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import org.jdom.Element

@Service(Service.Level.APP)
@State(name = "com.github.rebel000.cmdlineargs.sharedargs", category = SettingsCategory.PLUGINS, storages = [Storage("CommandlineArgs.xml", roamingType = RoamingType.DEFAULT)])
class ArgumentsGlobalStorage : SimplePersistentStateComponent<ArgumentsGlobalStorage.State>(State()) {
    companion object {
        fun getInstance(): ArgumentsGlobalStorage = ApplicationManager.getApplication().getService(ArgumentsGlobalStorage::class.java)
    }

    class State : BaseState() {
        var revision by property(ArgumentsService.SERIALIZE_REVISION)

        // deprecated: use args
        var sharedArgs by string()

        var showSharedNode by property(false)
        var showExperimental by property(false)
        var showUnsupported by property(true)

        var sharedArguments by property(Element("shared-arguments")) { it.isEmpty }
    }
}