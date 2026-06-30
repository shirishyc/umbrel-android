package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.WifiConnectionStatus
import com.umbrel.android.data.models.WifiNetwork
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun scan(): Result<List<WifiNetwork>> = runCatching {
        trpc.query("wifi.scan", deserializer = serializer<List<WifiNetwork>>()).getOrThrow()
    }

    suspend fun getNetworks(): Result<List<WifiNetwork>> = runCatching {
        trpc.query("wifi.networks", deserializer = serializer<List<WifiNetwork>>()).getOrThrow()
    }

    suspend fun getConnected(): Result<WifiConnectionStatus?> = runCatching {
        trpc.query("wifi.connected", deserializer = serializer<WifiConnectionStatus?>()).getOrThrow()
    }

    suspend fun connect(ssid: String, password: String? = null): Result<Unit> = runCatching {
        val params = mutableMapOf("ssid" to JsonPrimitive(ssid))
        password?.let { params["password"] = JsonPrimitive(it) }
        trpc.mutation("wifi.connect", params = params, deserializer = serializer<JsonNull>()).getOrThrow()
    }

    suspend fun disconnect(): Result<Unit> = runCatching {
        trpc.mutation("wifi.disconnect", deserializer = serializer<JsonNull>()).getOrThrow()
    }
}
