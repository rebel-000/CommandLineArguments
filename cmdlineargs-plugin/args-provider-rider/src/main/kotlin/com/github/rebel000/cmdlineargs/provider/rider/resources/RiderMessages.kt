package com.github.rebel000.cmdlineargs.provider.rider.resources

import com.intellij.AbstractBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object RiderMessages : AbstractBundle(RiderMessages.BUNDLE) {
    @NonNls
    private const val BUNDLE = "messages.CommandlineArgsRiderBundle"

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}
