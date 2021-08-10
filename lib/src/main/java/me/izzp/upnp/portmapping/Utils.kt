package me.izzp.upnp.portmapping

import java.net.Inet4Address
import java.net.NetworkInterface

fun NetworkInterface.defaultIP4Address(): Inet4Address? {
    this.inetAddresses?.iterator()?.forEach {
        if (it is Inet4Address) {
            return it
        }
    }
    return null
}

object Utils {
    fun findDefaultNetworkInterface(): NetworkInterface? {
        NetworkInterface.getNetworkInterfaces().iterator().forEach { ni ->
            if (ni.isLoopback) {
                return@forEach
            }
            if (!ni.isUp) {
                return@forEach
            }
            if (ni.isPointToPoint) {
                return@forEach
            }
            if (!ni.supportsMulticast()) {
                return@forEach
            }
            val list = ni.inetAddresses.iterator()
            list.forEach { addr ->
                if (addr is Inet4Address) {
                    println("defaultNetworkInterface:$ni")
                    return ni
                }
            }
        }
        return null
    }
}