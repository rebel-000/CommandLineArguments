package com.github.rebel000.cmdlineargs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "com.github.rebel000.cmdlineargs.sharedargs", category = SettingsCategory.PLUGINS, storages = [Storage("CommandlineArgs.xml", roamingType = RoamingType.DEFAULT)])
class ArgumentsSharedStorage : SimplePersistentStateComponent<ArgumentsSharedStorage.State>(State()) {
    companion object {
        fun getInstance(): ArgumentsSharedStorage = ApplicationManager.getApplication().getService(ArgumentsSharedStorage::class.java)
    }

    class State : BaseState() {
        var sharedArgs by string()
        var revision by property(0)
        var showSharedNode by property(false)
        var showPreviewNode by property(false)
    }
}