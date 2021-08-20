package com.github.zzp.upnp.portmapping

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

    fun enableLog(enable: Boolean) {
        Logger.enable = enable
    }

    fun clearLog() {
        Logger.clear()
    }

    fun logs() = Logger.getAll()

    fun cancel() {
        Udp.cancel()
    }

    fun clear() {
        Udp.cancel()
        gateway = ""
    }
}