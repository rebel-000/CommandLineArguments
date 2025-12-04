package com.github.rebel000.cmdlineargs.resources

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object ActionMessages : AbstractBundle(ActionMessages.BUNDLE) {
    @NonNls
    private const val BUNDLE = "messages.CommandlineArgsActionsBundle"

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}
