package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.resources.Messages
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JLabel

class SharedWarningDialog : DialogWrapper(true) {
    private val warningCheck = JBCheckBox(Messages.message("toolwindow.showShared.check"))

    init {
        title = Messages.message("toolwindow.showShared.title")
        isResizable = false

        warningCheck.addItemListener {
            myOKAction.isEnabled = warningCheck.isSelected
        }
        myOKAction.isEnabled = false
        warningCheck.alignmentX = Component.RIGHT_ALIGNMENT

        init()
    }
    override fun createCenterPanel(): JComponent? {
        setSize(310, 0)
        return FormBuilder
            .createFormBuilder()
            .addComponent(JLabel(Messages.message("toolwindow.showShared.message")).apply {
                foreground = JBColor.RED
            })
            .addComponent(JLabel(""))
            .addLabeledComponent(JLabel(""), warningCheck)
            .panel
    }

    override fun doOKAction() {
        super.doOKAction()
    }
}