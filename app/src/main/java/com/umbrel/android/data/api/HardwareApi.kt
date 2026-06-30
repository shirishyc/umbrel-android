package com.umbrel.android.data.api

import com.umbrel.android.core.network.TrpcClient
import com.umbrel.android.data.models.HardwareInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareApi @Inject constructor(
    private val trpc: TrpcClient,
) {
    suspend fun get(): Result<HardwareInfo> = runCatching {
        trpc.query("hardware.get", deserializer = serializer()).getOrThrow()
    }

    suspend fun getCpuTemperature(): Result<Double> = runCatching {
        trpc.query("hardware.cpuTemperature", deserializer = serializer()).getOrThrow()
    }
}
