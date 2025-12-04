package com.github.rebel000.cmdlineargs.serialization.xml

import com.github.rebel000.cmdlineargs.serialization.ArrayWriter
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import org.jdom.Element

internal class XmlArrayWriter(key: String, parent: Element? = null) : ArrayWriter {
    val xElement = Element(key)

    init {
        parent?.addContent(xElement)
    }

    override fun add(value: Boolean) { xElement.addContent(Element("item").addContent(value.toString())) }
    override fun add(value: Int) { xElement.addContent(Element("item").addContent(value.toString())) }
    override fun add(value: Float) { xElement.addContent(Element("item").addContent(value.toString())) }
    override fun add(value: String) { xElement.addContent(Element("item").addContent(value)) }
    override fun addArray(capacity: Int): ArrayWriter = XmlArrayWriter("item", xElement)
    override fun addObject(): ObjectWriter = XmlObjectWriter("item", xElement)

    override fun toString(): String = xElement.toString()
}