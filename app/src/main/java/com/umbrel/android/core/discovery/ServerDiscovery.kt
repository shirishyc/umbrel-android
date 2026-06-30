package com.umbrel.android.core.discovery

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

/**
 * Discovers UmbrelOS servers on the local network using mDNS (Bonjour/Zeroconf).
 *
 * Uses JmDNS library to browse for `_http._tcp` services, filtering for
 * those that identify as UmbrelOS.
 *
 * UmbrelOS typically advertises as:
 *   - Service type: _http._tcp.local.
 *   - Service name: "umbrel-*" or containing "Umbrel"
 */
@Singleton
class ServerDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var jmdns: JmDNS? = null

    /**
     * Flow that emits discovered UmbrelOS servers.
     * Emits each server as it's found on the network.
     */
    fun discoverServers(): Flow<DiscoveredServer> = callbackFlow {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiInfo = wifiManager?.connectionInfo

        try {
            // Get local IP address from WiFi connection
            val localIp = wifiInfo?.ipAddress?.let { intToIp(it) } ?: "127.0.0.1"
            val localHost = InetAddress.getByName(localIp)

            // Initialize JmDNS with the local WiFi address
            val instance = JmDNS.create(localHost, "UmbrelAndroidClient")
            jmdns = instance

            val listener = object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                    // Request service info — triggers serviceResolved
                    instance.requestServiceInfo(event.type, event.name)
                }

                override fun serviceRemoved(event: ServiceEvent) {
                    // Server went offline — could emit removal signal
                }

                override fun serviceResolved(event: ServiceEvent) {
                    val info: ServiceInfo = event.info
                    val serverName = info.name

                    // Filter for UmbrelOS — look for "Umbrel" in the server name
                    // or check for umbrel-specific TXT records
                    if (isUmbrelServer(serverName, info)) {
                        val address = info.inetAddresses.firstOrNull()?.hostAddress
                            ?: return

                        val server = DiscoveredServer(
                            name = serverName.removeSuffix("._http._tcp.local."),
                            host = info.server ?: address,
                            ipAddress = address,
                            port = info.port,
                            url = "http://$address:${info.port}",
                            version = info.getTextString("version"),
                        )
                        trySend(server)
                    }
                }
            }

            // Browse for HTTP services on the local network
            instance.addServiceListener("_http._tcp.local.", listener)

            // Also browse for HTTPS
            instance.addServiceListener("_https._tcp.local.", listener)

        } catch (e: Exception) {
            // JmDNS initialization failed — network might not be available
            close(e)
            return@callbackFlow
        }

        awaitClose {
            jmdns?.unregisterAllServices()
            jmdns?.close()
            jmdns = null
        }
    }

    /**
     * Simple mDNS resolution for "umbrel.local" as a fallback.
     */
    suspend fun resolveUmbrelLocal(): DiscoveredServer? {
        return try {
            val address = InetAddress.getByName("umbrel.local")
            DiscoveredServer.fromHost(
                name = "umbrel.local",
                host = address.hostAddress ?: "umbrel.local",
                port = 80,
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Check if an mDNS service is an UmbrelOS instance.
     */
    private fun isUmbrelServer(name: String, info: ServiceInfo): Boolean {
        // Check by name
        if (name.contains("umbrel", ignoreCase = true) ||
            name.contains("umbrelos", ignoreCase = true)
        ) return true

        // Check TXT records for umbrel-specific fields
        val serverField = info.getTextString("server")
        if (serverField?.contains("umbrel", ignoreCase = true) == true) return true

        return false
    }

    /**
     * Stop discovery and release resources.
     */
    fun stop() {
        jmdns?.unregisterAllServices()
        jmdns?.close()
        jmdns = null
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }
}
