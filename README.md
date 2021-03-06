# upnp.portmapping
upnp 端口映射 for android

[![](https://jitpack.io/v/tonyzzp/upnp.portmapping.svg)](https://jitpack.io/#tonyzzp/upnp.portmapping)


使用方式
```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

```gradle
dependencies {
  implementation 'com.github.tonyzzp:upnp.portmapping:1.4'
}
```


查找upnp网关地址
```kotlin
Upnp.requestGateway{gateway->
  println(gateway)
}
```


查找已经存在的端口映射
```kotlin
PortMapping.query(34567, "UDP"){result->
  println(result)
}
```


增加映射
```kotlin
PortMapping.add(externalPort, "UDP", internalHostIP, internalPort, "DESC"){result->
  println(result)
}
```


删除映射
```kotlin
PortMapping.del(externalPort, UDP_or_TCP){result->
  println(result)
}
```


停止接收udp广播(搜索upnp服务需要使用udp广播，可能会花费比较多时间，调用此方法可以停止搜索)
```kotlin
Upnp.cancel()
```

清除gateway缓存(gateway会在内存中有缓存，切换连接的路由器后需要调用此方法清除缓存)
```kotlin
Upnp.clear()
```

查询本机内网ip
```kotlin
 val ip = Utils.findDefaultNetworkInterface()?.defaultIP4Address()?.hostAddress
```
