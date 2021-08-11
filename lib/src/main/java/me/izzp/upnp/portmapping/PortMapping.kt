package me.izzp.upnp.portmapping

import android.content.Context
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread

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

    private fun request(context: Context, f: VoidFunction) {
        if (Upnp.gateway == "") {
            init(context)
            tasks.add(f)
        } else {
            f()
        }
    }

    fun query(context: Context, externalPort: Int, protocol: String, cb: (Info?) -> Unit) {
        request(context) {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "GetSpecificPortMappingEntry", mapOf(
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
                                externalPort, protocol, params["NewPortMappingDescription"] ?: ""
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
        context: Context,
        externalPort: Int,
        protocol: String,
        internalHost: String,
        internalPort: Int,
        desc: String,
        cb: (AddPortMappingResult) -> Unit
    ) {
        request(context) {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "AddPortMapping", mapOf(
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
        context: Context,
        externalPort: Int,
        protocol: String,
        cb: (success: Boolean) -> Unit
    ) {
        request(context) {
            thread {
                val response = Http.sendUPNPRequest(
                    Upnp.gateway, "DeletePortMapping", mapOf(
                        "NewExternalPort" to externalPort.toString(),
                        "NewProtocol" to protocol,
                    )
                )
                cb(response.code == 200)
            }
        }
    }

    private fun resolveResult(content: String): Map<String, String> {
        val rtn = mutableMapOf<String, String>()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(content.byteInputStream())?.documentElement
        val body = doc?.getElementsByTagName("s:Body")?.item(0) as Element?
        val nodes = body?.firstChild?.childNodes
        return if (nodes != null) {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                rtn[node.nodeName] = node.textContent
            }
            rtn
        } else {
            mapOf()
        }
    }

    private fun init(context: Context) {
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