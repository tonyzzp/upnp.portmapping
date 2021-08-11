package me.izzp.upnp.portmapping.demo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.izzp.upnp.portmapping.PortMapping
import me.izzp.upnp.portmapping.Upnp
import me.izzp.upnp.portmapping.Utils
import me.izzp.upnp.portmapping.defaultIP4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var gateway = ""
    private val tv by lazy { findViewById<TextView>(R.id.tv_gateway) }
    private val sp by lazy { getSharedPreferences("config", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gateway = sp.getString("gateway", "")!!
        tv.text = "gateway: $gateway"
    }

    override fun onDestroy() {
        super.onDestroy()
        Upnp.clear()
    }

    fun onNetworkInterfaceClick(view: View) {
        NetworkInterface.getNetworkInterfaces().iterator().forEach {
            dumpNetworkInterface(it)
            println("sub->")
            it.subInterfaces?.iterator()?.forEach {
                it?.also { it }
            }
        }
    }

    private fun dumpNetworkInterface(it: NetworkInterface) {
        println("-----")
        println(it.index)
        println(it.name)
        println(it.displayName)
        println("loopback:${it.isLoopback}")
        println("up:${it.isUp}")
        println("virtual:${it.isVirtual}")
        println("multicast:${it.supportsMulticast()}")
        println("p2p:${it.isPointToPoint}")
        println(
            "interfaceAddress: ${
                it.interfaceAddresses?.map { it.address.toString() }?.joinToString()
            }"
        )
    }

    fun onRequestGatewayClick(view: View) {
        gateway = ""
        tv.text = "gateway: 查找中"
        Upnp.requestGateway {
            gateway = it
            sp.edit().putString("gateway", gateway).apply()
            runOnUiThread {
                if (it != "") {
                    tv.text = "gateway: $it"
                } else {
                    tv.text = "gateway: 未找到"
                }
            }
        }
    }

    fun onQueryClick(view: View) {
        PortMapping.query(this, 34567, "UDP") {
            runOnUiThread { alert(it?.toString() ?: "") }
        }
    }

    fun onAddPortMappingClick(view: View) {
        val ip = Utils.findDefaultNetworkInterface()?.defaultIP4Address()?.hostAddress ?: ""
        PortMapping.add(this, 34567, "UDP", ip, 34567, "test 34567") {
            runOnUiThread { alert(it.toString()) }
        }
    }

    fun onRemovePortMappingClick(view: View) {
        PortMapping.del(this, 34567, "UDP") {
            runOnUiThread { alert(it.toString()) }
        }
    }
}