package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import javax.swing.Icon

open class ArgumentTreeNodeBase(var text: String) : CheckedTreeNode() {
    @Suppress("unused")
    companion object {
        enum class ControlType {
            NONE, CHECKBOX, CHECKBOX_LOCKED, RADIOBUTTON
        }

        val SUCCESS_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.IconBadge.SUCCESS)

        val SUCCESS_BOLD_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBUI.CurrentTheme.IconBadge.SUCCESS)

        val INFORMATION_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.IconBadge.INFORMATION)

        val INFORMATION_BOLD_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBUI.CurrentTheme.IconBadge.INFORMATION)

        val WARN_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.IconBadge.WARNING)

        val WARN_BOLD_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBUI.CurrentTheme.IconBadge.WARNING)

        val ERROR_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.IconBadge.ERROR)

        val ERROR_BOLD_TEXT_ATTRIBUTES =
            SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBUI.CurrentTheme.IconBadge.ERROR)
    }

    open val controlType: ControlType get() = ControlType.NONE
    open val readonly: Boolean get() = true
    open var icon: Icon? = null
    var isExpanded: Boolean = true
    open var style: SimpleTextAttributes? = null
    var tooltip: String? = null

    override fun toString(): String {
        return text
    }
}
