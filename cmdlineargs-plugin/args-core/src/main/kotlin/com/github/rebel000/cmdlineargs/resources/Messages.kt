package com.github.rebel000.cmdlineargs.resources

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object Messages : AbstractBundle(Messages.BUNDLE) {
    @NonNls
    private const val BUNDLE = "messages.CommandlineArgsBundle"

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}
