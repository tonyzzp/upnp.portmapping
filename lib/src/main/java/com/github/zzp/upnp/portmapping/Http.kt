package com.github.zzp.upnp.portmapping

import java.net.HttpURLConnection
import java.net.URL

internal const val REQ_BODY = """<?xml version="1.0"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body>
<u:__ACTION__ xmlns:u="urn:schemas-upnp-org:service:WANIPConnection:1">
__PARAMS__
</u:__ACTION__>
</s:Body>
</s:Envelope>
"""

internal object Http {

    data class Response(val code: Int, val body: String)

    fun get(url: String): String = try {
        Logger.log("Http.get:$url")
        (URL(url).openConnection() as HttpURLConnection).let { conn ->
            conn.requestMethod = "GET"
            val result = conn.inputStream.readBytes().decodeToString()
            Logger.log(conn.responseCode)
            Logger.log(result)
            result
        }
    } catch (e: Exception) {
        Logger.log(e)
        e.printStackTrace()
        ""
    }

    fun post(url: String, headers: Map<String, String>, body: String): Response = try {
        Logger.log("Http.post: $url")
        Logger.log(headers)
        Logger.log(body)
        (URL(url).openConnection() as HttpURLConnection).let { conn ->
            conn.requestMethod = "POST"
            headers.forEach {
                conn.setRequestProperty(it.key, it.value)
            }
            conn.outputStream.use {
                it.write(body.toByteArray())
            }
            val code = conn.responseCode
            val stream = if (code >= 200 && code < 300) {
                conn.inputStream
            } else {
                conn.errorStream
            }
            val result = stream.readBytes().decodeToString()
            Logger.log(conn.responseCode)
            Logger.log(result)
            Response(code, result)
        }
    } catch (e: Exception) {
        Response(0, "")
    }

    fun sendUPNPRequest(url: String, action: String, params: Map<String, String>): Response {
        val sparams = params.map { "<${it.key}>${it.value}</${it.key}>" }.joinToString("")
        val body = REQ_BODY.replace("__ACTION__", action).replace("__PARAMS__", sparams)
        return post(
            url, mapOf(
                "Content-Type" to "text/html",
                "Cache-Control" to "no-cache",
                "Progma" to "no-cache",
                "soapaction" to "\"urn:schemas-upnp-org:service:WANIPConnection:1#$action\""
            ), body
        )
    }
}