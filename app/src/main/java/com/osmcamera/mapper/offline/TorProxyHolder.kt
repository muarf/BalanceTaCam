package com.osmcamera.mapper.offline

/**
 * Runtime flag consulted by the main OkHttp client's ProxySelector.
 * Kept separate from DataStore so networking code never blocks on IO.
 */
object TorProxyHolder {
    const val HOST = "127.0.0.1"
    const val PORT = 9050 // Orbot SOCKS5

    @Volatile
    var enabled: Boolean = false
}
