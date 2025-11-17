package com.github.rebel000.cmdlineargs.serialization.xml

import com.github.rebel000.cmdlineargs.serialization.EntryValue
import com.github.rebel000.cmdlineargs.serialization.ObjectReader
import org.jdom.Element

internal class XmlObjectReader(val xElement: Element) : ObjectReader {
    override fun count(): Int = xElement.children.count()
    override fun get(key: String): EntryValue = XmlEntryValue(xElement.getChild(key))
    override fun iterator(): Iterator<Pair<String, EntryValue>> {
        return object : Iterator<Pair<String, EntryValue>> {
            val it = xElement.children.iterator()
            override fun next(): Pair<String, EntryValue> {
                val element = it.next()
                return Pair(element.name, XmlEntryValue(element)) 
            }
            override fun hasNext(): Boolean = it.hasNext()
        }
    }
}