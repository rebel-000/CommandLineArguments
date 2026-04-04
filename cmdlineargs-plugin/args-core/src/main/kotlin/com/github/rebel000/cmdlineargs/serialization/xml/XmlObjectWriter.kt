package com.github.rebel000.cmdlineargs.serialization.xml

import com.github.rebel000.cmdlineargs.serialization.ArrayWriter
import com.github.rebel000.cmdlineargs.serialization.ObjectWriter
import org.jdom.Element

internal class XmlObjectWriter(key: String, parent: Element? = null) : ObjectWriter {
    val xElement = Element(key)

    init {
        parent?.addContent(xElement)
    }

    override fun set(key: String, value: Boolean) { xElement.addContent(Element(key).addContent(value.toString())) }
    override fun set(key: String, value: Int) { xElement.addContent(Element(key).addContent(value.toString())) }
    override fun set(key: String, value: Float) { xElement.addContent(Element(key).addContent(value.toString())) }
    override fun set(key: String, value: String) { xElement.addContent(Element(key).addContent(value)) }
    override fun addArray(key: String, capacity: Int): ArrayWriter = XmlArrayWriter(key, xElement)
    override fun addObject(key: String): ObjectWriter = XmlObjectWriter(key, xElement)

    override fun toString(): String = xElement.toString()
}