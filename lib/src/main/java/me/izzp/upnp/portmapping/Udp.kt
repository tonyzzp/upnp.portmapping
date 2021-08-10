package me.izzp.upnp.portmapping

import android.content.Context
import android.net.wifi.WifiManager
import org.w3c.dom.Element
import java.net.*
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

internal const val ST_GATEWAY = "urn:schemas-upnp-org:device:InternetGatewayDevice:1"
internal const val ST_ROOTDEVICE = "upnp:rootdevice"

internal const val MSG_MSEARCH = """M-SEARCH * HTTP/1.1
HOST: 239.255.255.250:1900
ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1
MAN: "ssdp:discover"
MX: 1
"""

internal object Udp {

    private var socket: MulticastSocket? = null
    private var upnpUrl = ""
    private var timer: Timer? = null
    private var lock: WifiManager.MulticastLock? = null
    private var cb: ((String) -> Unit)? = null

    private fun listen() {
        if (socket != null) {
            return
        }
        socket = MulticastSocket(1900)
        socket!!.joinGroup(InetAddress.getByName("239.255.255.250"))
        val s = socket!!
        thread {
            println("listen.run")
            val bytes = ByteArray(1024 * 10)
            val packet = DatagramPacket(bytes, bytes.size)
            while (true) {
                try {
                    s.receive(packet)
                } catch (e: Exception) {
                    s.close()
                    socket = null
                    break
                }
                val str = packet.data.decodeToString(packet.offset, packet.length)
                println("------------")
                println(str)
                val lines = str.split("\n").map { it.trim() }
                if (!lines[0].startsWith("HTTP/1.1") && !lines[0].startsWith("NOTIFY")) {
                    continue
                }
                val headers = parseHeaders(lines)
                if (headers["st"] == ST_GATEWAY || headers["st"] == ST_ROOTDEVICE || headers["nt"] ==
                    ST_ROOTDEVICE
                ) {
                    val url = headers["location"]
                    if (url != null && url.isNotBlank()) {
                        upnpUrl = url
                        println("upnp:$upnpUrl")
                        onUpnpResolved()
                        break
                    }
                }
            }
        }
    }

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

    private fun send() {
        println("send")
        val bytes = MSG_MSEARCH.toByteArray()
        val packet =
            DatagramPacket(bytes, 0, bytes.size, InetSocketAddress("239.255.255.250", 1900))
        socket?.send(packet)
    }

    private fun onUpnpResolved() {
        lock?.release()
        timer?.cancel()
        timer = null
        socket?.close()
        socket = null
        val content = Http.get(upnpUrl)
        if (content == "") {
            cb?.invoke("")
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
        cb?.invoke(gateway)
        cb = null
    }

    fun requestGateway(context: Context, cb: (String) -> Unit) {
        if (timer != null) {
            return
        }
        val mgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        lock = mgr.createMulticastLock("upnp")
        lock?.acquire()
        this.cb = cb
        listen()
        var times = 0
        timer = Timer()
        timer?.schedule(0L, 5000L) {
            times++
            if (times > 20) {
                cancel()
                cb.invoke("")
                this@Udp.cb = null
            } else {
                send()
            }
        }
    }

    fun cancel() {
        timer?.cancel()
        timer = null
        socket?.close()
        socket = null
    }
}