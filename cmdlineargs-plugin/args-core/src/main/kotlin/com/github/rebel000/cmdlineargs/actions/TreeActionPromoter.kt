package com.github.rebel000.cmdlineargs.actions

import com.github.rebel000.cmdlineargs.ui.ArgumentDataContext
import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext

class TreeActionPromoter : ActionPromoter {
    override fun promote(actions: List<AnAction?>, context: DataContext): List<AnAction?> {
        if (context.getData(ArgumentDataContext.KEY) != null) {
            return actions.filter { it is TreeAction }.ifEmpty { actions }
        }
        return actions
    }
}