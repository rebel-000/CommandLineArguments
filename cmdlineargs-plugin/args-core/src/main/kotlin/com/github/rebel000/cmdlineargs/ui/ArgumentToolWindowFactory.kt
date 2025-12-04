package com.github.rebel000.cmdlineargs.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.impl.ContentImpl

class ArgumentToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val view = ArgumentToolWindowPanel(project)
        toolWindow.setTitleActions(view.getTitleActions())
        toolWindow.setAdditionalGearActions(view.getExtraActions())
        toolWindow.contentManager.addContent(ContentImpl(view, null, true))
    }
}