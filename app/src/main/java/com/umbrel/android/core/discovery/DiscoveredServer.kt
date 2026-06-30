package com.umbrel.android.core.discovery

/**
 * Represents an UmbrelOS server discovered on the local network.
 */
data class DiscoveredServer(
    val name: String,
    val host: String,
    val ipAddress: String,
    val port: Int,
    val url: String,
    val version: String? = null,
) {
    companion object {
        fun fromHost(name: String, host: String, port: Int) = DiscoveredServer(
            name = name,
            host = host,
            ipAddress = host,
            port = port,
            url = "http://$host:$port",
        )
    }
}
