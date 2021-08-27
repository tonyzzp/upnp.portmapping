package com.github.zzp.upnp.portmapping

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

internal class NodeListIterator(private val nodeList: NodeList) : Iterator<Node> {

    private var index = 0

    override fun hasNext() = index < nodeList.length

    override fun next() = nodeList.item(index++)
}

internal class NodeListIterable(private val nodeList: NodeList) : Iterable<Node> {
    override fun iterator() = NodeListIterator(nodeList)
}

internal fun NodeList.asIterable() = NodeListIterable(this)

internal fun Element.getElementByTagName(tag: String) =
    this.getElementsByTagName(tag).item(0) as Element?
