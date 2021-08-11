package me.izzp.upnp.portmapping

typealias VoidFunction = () -> Unit

object Upnp {

    var gateway = ""

    fun requestGateway(cb: (String) -> Unit) {
        if (gateway != "") {
            cb(gateway)
        } else {
            Udp.requestGateway {
                gateway = it
                cb(it)
            }
        }
    }

    fun cancel() {
        Udp.cancel()
    }

    fun clear() {
        Udp.cancel()
        gateway = ""
    }
}