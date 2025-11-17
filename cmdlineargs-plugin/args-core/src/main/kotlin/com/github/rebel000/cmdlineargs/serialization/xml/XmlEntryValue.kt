package com.github.rebel000.cmdlineargs.serialization.xml

import com.github.rebel000.cmdlineargs.serialization.ArrayReader
import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import org.jdom.Element

internal class XmlEntryValue(val xElement: Element?) : EntryValue {
    override val asBoolean: Boolean? get() = xElement?.value?.lowercase()?.toBooleanStrictOrNull()
    override val asInt: Int? get() = xElement?.value?.toIntOrNull()
    override val asFloat: Float? get() = xElement?.value?.toFloatOrNull()
    override val asString: String? get() = xElement?.value
    override val asObject: ObjectReader? get() = xElement?.let { XmlObjectReader(it) }
    override val asArray: ArrayReader? get() =  xElement?.let { XmlArrayReader(it) }
}

