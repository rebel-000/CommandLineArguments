package com.github.rebel000.cmdlineargs.tree

import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import javax.swing.Icon

internal class InfoNode(var text: String, var icon: Icon? = null, var style: SimpleTextAttributes? = null) : ArgumentTreeNodeBase() {
    @Suppress("unused")
    companion object {
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

    var userdata: Any? = null

    init {
        isEnabled = false
    }

    override fun toString(): String {
        return text
    }
}
