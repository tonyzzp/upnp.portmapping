package me.izzp.upnp.portmapping.demo

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

typealias VoidFunction = () -> Unit

fun Activity.alert(
    message: String,
    positiveLabel: String? = null,
    positiveListener: VoidFunction? = null,
    negativeLabel: String? = null,
    negativeListener: VoidFunction? = null
) {
    val builder = AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton(positiveLabel ?: "ok") { dialogInterface: DialogInterface, i: Int ->
            positiveListener?.invoke()
        }
    if (negativeListener != null) {
        builder.setNegativeButton(
            negativeLabel ?: "cancel"
        ) { dialogInterface: DialogInterface, i: Int ->
            negativeListener()
        }
    }
    builder.show()
}