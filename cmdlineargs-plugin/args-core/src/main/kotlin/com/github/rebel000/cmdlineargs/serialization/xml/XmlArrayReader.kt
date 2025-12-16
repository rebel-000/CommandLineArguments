package com.github.rebel000.cmdlineargs.serialization.xml

import com.github.rebel000.cmdlineargs.serialization.ArrayReader
import com.github.rebel000.cmdlineargs.serialization.EntryValue
import org.jdom.Element

internal class XmlArrayReader(val xElement: Element) : ArrayReader {
    override fun count(): Int = xElement.children.count()
    override fun get(index: Int): EntryValue = XmlEntryValue(xElement.children[index])
    override fun iterator(): Iterator<EntryValue> {
        return object : Iterator<EntryValue> {
            val it = xElement.children.iterator()
            override fun next(): EntryValue = XmlEntryValue(it.next())
            override fun hasNext(): Boolean = it.hasNext()
        }
    }
}