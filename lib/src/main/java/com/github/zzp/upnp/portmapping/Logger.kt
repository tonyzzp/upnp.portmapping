package com.github.zzp.upnp.portmapping

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

internal object Logger {

    private val sb = StringBuilder()
    var enable = false

    fun log(msg: Any) {
        if (!enable) {
            return
        }
        Log.d("UPNP", msg.toString())
        sb.append("[")
        sb.append(sdf.format(Date()))
        sb.append("]")
        sb.appendLine(msg)
    }

    fun clear() {
        sb.clear()
    }

    fun getAll() = sb.toString()
}