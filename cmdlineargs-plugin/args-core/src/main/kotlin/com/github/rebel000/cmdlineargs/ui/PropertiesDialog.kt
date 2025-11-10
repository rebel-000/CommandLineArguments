package com.github.rebel000.cmdlineargs.ui

import com.github.rebel000.cmdlineargs.ArgumentsService
import com.github.rebel000.cmdlineargs.resources.Messages
import com.github.rebel000.cmdlineargs.tree.ArgumentNode
import com.github.rebel000.cmdlineargs.tree.visitors.CollectArgsVisitor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.DimensionService
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Suppress("DialogTitleCapitalization")
internal class PropertiesDialog(private val project: Project, private val node: ArgumentNode) : DialogWrapper(true) {
    companion object {
        const val DIALOG_ID = "com.github.rebel000.cmdlineargs.ui.PropertiesDialog"
    }

    private val nameField: JBTextField = JBTextField(node.name)
    private val descField: JBTextField = JBTextField(node.description)
    private val isFolderField: JBCheckBox = JBCheckBox(Messages.message("properties.isFolder"), node.isFolder)
    private val isParameterField: JBCheckBox = JBCheckBox(Messages.message("properties.isParameter"), node.isParameter)
    private val isSingleChoiceField: JBCheckBox = JBCheckBox(Messages.message("properties.isSingle"), node.isSingle)
    private val joinChildrenField: JBCheckBox = JBCheckBox(Messages.message("properties.enable"), node.join)
    private val joinSeparatorField: JBTextField = JBTextField(node.joinSeparator)
    private val joinPrefixField: JBTextField = JBTextField(node.joinPrefix)
    private val joinPostfixField: JBTextField = JBTextField(node.joinPostfix)
    private val previewNode = ArgumentNode(nameField.text)
    private val previewField: JBTextField = JBTextField()
    private val filterGroups = createFilterGroups()
    private var suspended = false

    init {
        super.init()
        title = Messages.message("properties.title")
        val dimensionService: DimensionService = DimensionService.getInstance()
        dimensionService.getSize(DIALOG_ID, project)?.let { setSize(it.width, it.height) }
        dimensionService.getLocation(DIALOG_ID, project)?.let { location = it }
        val actionListener = ActionListener { update() }
        val textFieldListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = update()
            override fun removeUpdate(e: DocumentEvent?) = update()
            override fun changedUpdate(e: DocumentEvent?) = update()
        }
        nameField.document.addDocumentListener(textFieldListener)
        isFolderField.addActionListener(actionListener)
        isSingleChoiceField.addActionListener(actionListener)
        isParameterField.addActionListener(actionListener)
        joinChildrenField.addActionListener(actionListener)
        joinSeparatorField.maximumSize = Dimension(50, Int.MAX_VALUE)
        joinSeparatorField.document.addDocumentListener(textFieldListener)
        joinPrefixField.maximumSize = Dimension(50, Int.MAX_VALUE)
        joinPrefixField.document.addDocumentListener(textFieldListener)
        joinPostfixField.maximumSize = Dimension(50, Int.MAX_VALUE)
        joinPostfixField.document.addDocumentListener(textFieldListener)
        previewNode.isFolder = isFolderField.isSelected
        previewNode.add(ArgumentNode("value1"))
        previewNode.add(ArgumentNode("value2"))
        previewNode.add(ArgumentNode("value3"))
        previewNode.isChecked = false
        previewNode.isChecked = true
        previewField.isEditable = false
        previewField.border = BorderFactory.createEmptyBorder()
        previewField.isOpaque = false
        update()
    }

    private fun applyTo(node: ArgumentNode) {
        node.name = nameField.text
        node.description = descField.text
        node.isFolder = isFolderField.isSelected
        node.isParameter = isParameterField.isSelected
        node.isSingle = isSingleChoiceField.isSelected
        node.join = joinChildrenField.isSelected
        node.joinSeparator = joinSeparatorField.text
        node.joinPrefix = joinPrefixField.text
        node.joinPostfix = joinPostfixField.text
    }

    override fun createCenterPanel(): JComponent {
        val builder = FormBuilder
            .createFormBuilder()
            .addLabeledComponent(Messages.message("properties.name"), nameField)
            .addLabeledComponent(Messages.message("properties.desc"), descField)
            .addSeparator()
            .addComponentToRightColumn(isFolderField)
            .addComponentToRightColumn(isParameterField)
            .addComponentToRightColumn(isSingleChoiceField)
            .addSeparator()
            .addLabeledComponent(Messages.message("properties.join"), joinChildrenField)
            .addLabeledComponent(Messages.message("properties.joinSeparator"), joinSeparatorField)
            .addLabeledComponent(Messages.message("properties.joinPrefix"), joinPrefixField)
            .addLabeledComponent(Messages.message("properties.joinPostfix"), joinPostfixField)
            .addSeparator()
            .addLabeledComponent(Messages.message("properties.preview"), previewField)
            .addSeparator()
            .addComponent(JLabel(Messages.message("properties.filters")))
            .addComponent(JLabel(Messages.message("properties.filters.desc")))
            .addSeparator()
        for (f in filterGroups) {
            builder.addLabeledComponent(f.title, f.field)
                .addComponentToRightColumn(f.checkboxGroup)
                .addSeparator()
        }
        val panel = JBScrollPane(builder.panel)
        panel.border = BorderFactory.createEmptyBorder()
        panel.preferredSize = JBUI.size(970, 680)
        return panel
    }

    private fun createFilterGroups(): List<PropertiesFilterGroup> {
        return ArgumentsService.getInstance(project).getFilters().map {
            PropertiesFilterGroup(it, node)
        }.toList()
    }

    private fun update() {
        if (suspended) {
            return
        }
        suspended = true
        val isFolder = isFolderField.isSelected
        val isJoin = isFolder && joinChildrenField.isSelected
        isParameterField.isEnabled = isFolder
        joinChildrenField.isEnabled = isFolder
        isSingleChoiceField.isEnabled = isFolder
        joinSeparatorField.isEnabled = isJoin
        joinPrefixField.isEnabled = isJoin
        joinPostfixField.isEnabled = isJoin
        applyTo(previewNode)
        previewNode.isChecked = false
        previewNode.isChecked = true
        previewField.text = CollectArgsVisitor{ true }.let{ 
            previewNode.traverse(it)
            it.toString()
        }
        suspended = false
    }

    private fun saveDimensions() {
        val dimensionService = DimensionService.getInstance()
        val size = size
        val location = location
        dimensionService.setSize(DIALOG_ID, size, project)
        dimensionService.setLocation(DIALOG_ID, location, project)
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun doOKAction() {
        applyTo(node)
        node.filters = filterGroups.associate { Pair(it.key, it.items()) }
        saveDimensions()
        super.doOKAction()
    }

    override fun doCancelAction() {
        saveDimensions()
        super.doCancelAction()
    }

}

