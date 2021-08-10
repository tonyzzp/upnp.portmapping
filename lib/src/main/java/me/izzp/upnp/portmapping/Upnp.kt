package me.izzp.upnp.portmapping

import android.content.Context

typealias VoidFunction = () -> Unit

object Upnp {

    var gateway = ""

    fun requestGateway(context: Context, cb: (String) -> Unit) {
        if (gateway != "") {
            cb(gateway)
        } else {
            Udp.requestGateway(context) {
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