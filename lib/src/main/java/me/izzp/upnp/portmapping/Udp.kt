package me.izzp.upnp.portmapping

import org.w3c.dom.Element
import java.net.*
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread

internal const val ST_GATEWAY = "urn:schemas-upnp-org:device:InternetGatewayDevice:1"
internal const val ST_ROOTDEVICE = "upnp:rootdevice"
internal const val MSG_MSEARCH =
    "M-SEARCH * HTTP/1.1\r\nhost: 239.255.255.250:1900\r\nman: \"ssdp:discover\"\r\nst: upnp:rootdevice\r\nmx: 1\r\n\r\n"

internal object Udp {

    private var socket: DatagramSocket? = null
    private var cb: ((String) -> Unit)? = null

    private fun parseHeaders(lines: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        lines.forEach {
            val index = it.indexOf(":")
            if (index > -1) {
                val key = it.substring(0, index).trim().lowercase()
                val value = it.substring(index + 1).trim()
                map[key] = value
            }
        }
        return map
    }

    private fun onUpnpResolved(upnpUrl: String) {
        socket = null
        val content = Http.get(upnpUrl)
        if (content == "") {
            onGatewalResolved("")
            return
        }
        val doc =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(content.byteInputStream())
        val services = doc.documentElement.getElementsByTagName("service")
        for (i in 0 until services.length) {
            val service = services.item(i) as Element
            if (service.getElementsByTagName("serviceType")
                    .item(0).textContent == "urn:schemas-upnp-org:service:WANIPConnection:1"
            ) {
                val path = service.getElementsByTagName("controlURL").item(0).textContent
//                val host =
//                    doc.documentElement.getElementsByTagName("presentationURL").item(0).textContent
                val host = URL(upnpUrl).let {
                    "${it.protocol}://${it.host}:${it.port}"
                }
                var gateway = ""
                if (host.endsWith("/") && path.startsWith("/")) {
                    gateway = host.substring(0, host.length - 1) + path
                } else if (host.endsWith("/") && !path.startsWith("/")) {
                    gateway = host + path
                } else if (!host.endsWith("/") && path.startsWith("/")) {
                    gateway = host + path
                } else {
                    gateway = "$host/$path"
                }
                println(gateway)
                onGatewalResolved(gateway)
                break
            }
        }
    }

    private fun onGatewalResolved(gateway: String) {
        socket = null
        cb?.invoke(gateway)
        cb = null
    }

    fun requestGateway(cb: (String) -> Unit) {
        this.cb = cb
        thread {
            val socket = DatagramSocket()
            this.socket = socket
            val bytes = MSG_MSEARCH.toByteArray()
            val packet =
                DatagramPacket(bytes, bytes.size, InetSocketAddress("239.255.255.250", 1900))
            try {
                socket.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
                socket.close()
                onGatewalResolved("")
                return@thread
            }
            val readPacket = DatagramPacket(ByteArray(10240), 10240)
            try {
                socket.receive(readPacket)
            } catch (e: Exception) {
                e.printStackTrace()
                socket.close()
                onGatewalResolved("")
                return@thread
            }
            val content = readPacket.data.decodeToString(readPacket.offset, readPacket.length)
            val headers = parseHeaders(content.split("\n"))
            val location = headers["location"]
            socket.close()
            onUpnpResolved(location ?: "")
        }
    }

    fun cancel() {
        cb = null
        socket?.close()
        socket = null
    }
}