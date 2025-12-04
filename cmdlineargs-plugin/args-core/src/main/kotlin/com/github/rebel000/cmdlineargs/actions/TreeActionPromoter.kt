package com.github.rebel000.cmdlineargs.actions

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext

class TreeActionPromoter : ActionPromoter {
    override fun promote(actions: List<AnAction?>, context: DataContext): List<AnAction?> {
        if (context.getArgumentDataContext() != null) {
            return actions.filter { it is TreeAction }.ifEmpty { actions }
        }
        return actions
    }
}