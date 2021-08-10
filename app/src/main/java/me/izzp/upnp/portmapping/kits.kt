package me.izzp.upnp.portmapping

import android.app.Activity
import androidx.appcompat.app.AlertDialog

fun Activity.alert(message: String) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton("ok", null)
        .show()
}