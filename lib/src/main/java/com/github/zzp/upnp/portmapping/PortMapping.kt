package com.github.zzp.upnp.portmapping

import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread


val NODE_TYPES = mapOf(
    Node.ELEMENT_NODE to "ELEMENT_NODE",
    Node.ATTRIBUTE_NODE to "ATTRIBUTE_NODE",
    Node.TEXT_NODE to "TEXT_NODE",
    Node.CDATA_SECTION_NODE to "CDATA_SECTION_NODE",
    Node.ENTITY_REFERENCE_NODE to "ENTITY_REFERENCE_NODE",
    Node.ENTITY_NODE to "ENTITY_NODE",
    Node.PROCESSING_INSTRUCTION_NODE to "PROCESSING_INSTRUCTION_NODE",
    Node.COMMENT_NODE to "COMMENT_NODE",
    Node.DOCUMENT_NODE to "DOCUMENT_NODE",
    Node.DOCUMENT_TYPE_NODE to "DOCUMENT_TYPE_NODE",
    Node.DOCUMENT_FRAGMENT_NODE to "DOCUMENT_FRAGMENT_NODE",
    Node.NOTATION_NODE to "NOTATION_NODE",
)

object PortMapping {

    private val tasks = mutableListOf<VoidFunction>()

    data class Info(
        val internalHost: String,
        val internalPort: Int,
        val externalHost: String,
        val externalPort: Int,
        val protocol: String,
        val desc: String,
    )

    enum class AddPortMappingResult {
        success,
        fail,
        exist,
    }

    private fun request(f: VoidFunction) {
        if (Upnp.gateway == "") {
            init()
            tasks.add(f)
        } else {
            f()
        }
    }

    fun query(externalPort: Int, protocol: String, cb: (Info?) -> Unit) {
        Logger.log("query $externalPort $protocol")
        request {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "GetSpecificPortMappingEntry", mapOf(
                        "NewRemoteHost" to "",
                        "NewExternalPort" to externalPort.toString(),
                        "NewProtocol" to protocol,
                    )
                )
                when (response.code) {
                    500 -> {
                        cb(Info("", 0, "", externalPort, protocol, ""))
                    }
                    200 -> {
                        val params = resolveResult(response.body)
                        cb(
                            Info(
                                params["NewInternalClient"] ?: "",
                                params["NewInternalPort"]?.toInt() ?: 0,
                                "",
                                externalPort,
                                protocol,
                                params["NewPortMappingDescription"] ?: ""
                            )
                        )
                    }
                    else -> {
                        cb(null)
                    }
                }
            }
        }
    }

    fun add(
        externalPort: Int,
        protocol: String,
        internalHost: String,
        internalPort: Int,
        desc: String,
        cb: (AddPortMappingResult) -> Unit
    ) {
        request {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "AddPortMapping", mapOf(
                        "NewRemoteHost" to "",
                        "NewExternalPort" to externalPort.toString(),
                        "NewProtocol" to protocol,
                        "NewInternalPort" to internalPort.toString(),
                        "NewInternalClient" to internalHost,
                        "NewEnabled" to "1",
                        "NewPortMappingDescription" to desc,
                        "NewLeaseDuration" to "0",
                    )
                )
                when (response.code) {
                    200 -> {
                        cb(AddPortMappingResult.success)
                    }
                    500 -> {
                        cb(AddPortMappingResult.exist)
                    }
                    else -> {
                        cb(AddPortMappingResult.fail)
                    }
                }
            }
        }
    }

    fun del(
        externalPort: Int,
        protocol: String,
        cb: (success: Boolean) -> Unit
    ) {
        request {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "DeletePortMapping", mapOf(
                        "NewRemoteHost" to "",
                        "NewExternalPort" to externalPort.toString(),
                        "NewProtocol" to protocol,
                    )
                )
                cb(response.code == 200 || response.code == 500)
            }
        }
    }

    private fun resolveResult(content: String): Map<String, String> {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(content.byteInputStream())?.documentElement
            ?.getElementByTagName("s:Body")
            ?.childNodes?.asIterable()?.find { it is Element }
            ?.childNodes?.asIterable()?.filterIsInstance<Element>()
            ?.map { it.tagName to it.textContent }?.toMap() ?: mapOf()
    }

    private fun init() {
        thread {
            Upnp.requestGateway {
                if (it != "") {
                    Upnp.gateway = it
                    onInit()
                }
            }
        }
    }

    private fun onInit() {
        tasks.forEach {
            it()
        }
        tasks.clear()
    }
}