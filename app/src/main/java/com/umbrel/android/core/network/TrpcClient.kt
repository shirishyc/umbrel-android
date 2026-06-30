package com.umbrel.android.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Kotlin-native tRPC HTTP client for UmbrelOS.
 *
 * tRPC v10 HTTP transport:
 *   POST /trpc/{procedure}?batch=1
 *   Body: JSON input ({} for no params)
 *   Response: [{ "result": { "data": <value> } }]
 *
 * Auth: Authorization: Bearer <token> header
 */
@Singleton
class TrpcClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private var baseUrl: String = ""
    private var authToken: String? = null

    fun setBaseUrl(url: String) { baseUrl = url.trimEnd('/') }
    fun getBaseUrl(): String = baseUrl
    fun setAuthToken(token: String?) { authToken = token }

    /**
     * Step 1: Basic HTTP connectivity test.
     * Tries a simple GET to the root URL to verify the server is reachable.
     */
    suspend fun pingServer(): Result<String> = runCatching {
        val request = Request.Builder()
            .url(baseUrl)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val code = response.code
        val bodyPreview = response.body?.string()?.take(200) ?: "(empty)"
        "HTTP $code - ${bodyPreview.take(100)}"
    }

    /**
     * Step 2: Try a raw HTTP request and return the full response for debugging.
     */
    suspend fun rawTrpcCall(procedure: String, body: String = "[{}]"): Result<String> = runCatching {
        val request = Request.Builder()
            .url("$baseUrl/trpc/$procedure?batch=1")
            .post(body.toRequestBody("application/json".toMediaType()))
            .apply { authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "(empty)"
        "HTTP ${response.code} | Body: ${responseBody.take(500)}"
    }

    suspend fun checkConnectivity(): Result<Boolean> = runCatching {
        val data = executeRaw("system.status", null)
        data is JsonObject
    }

    suspend fun <T> query(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute(procedure, params, deserializer)

    suspend fun <T> mutation(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute(procedure, params, deserializer)

    private suspend fun <T> execute(
        procedure: String,
        params: Map<String, JsonElement>?,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = runCatching {
        val data = executeRaw(procedure, params)
        json.decodeFromJsonElement(deserializer, data)
    }

    private suspend fun executeRaw(
        procedure: String,
        params: Map<String, JsonElement>?,
    ): JsonElement {
        // When using batch=1, tRPC v10 requires the body to be a JSON ARRAY
        val inputJson = if (params != null) {
            // Wrap in array: [{"password":"xxx"}]
            val single = json.encodeToJsonElement(kotlinx.serialization.serializer<Map<String, JsonElement>>(), params)
            json.encodeToString(kotlinx.serialization.serializer<List<kotlinx.serialization.json.JsonElement>>(), listOf(single))
        } else {
            "[{}]"
        }

        val request = Request.Builder()
            .url("$baseUrl/trpc/$procedure?batch=1")
            .post(inputJson.toRequestBody("application/json".toMediaType()))
            .apply { authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from $procedure")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code} from POST /trpc/$procedure — body: ${responseBody.take(200)}")
        }

        // tRPC response is always an array: [{ "result": { "data": ... } }]
        val rootArray = try {
            json.parseToJsonElement(responseBody).jsonArray
        } catch (e: Exception) {
            throw Exception("Invalid JSON response from $procedure: ${responseBody.take(300)}")
        }

        if (rootArray.isEmpty()) {
            throw Exception("Empty array in response from $procedure: $responseBody")
        }

        val first = rootArray[0].jsonObject

        // Check for tRPC error object
        first["error"]?.let { errObj ->
            val msg = (errObj as? JsonObject)?.get("message")?.toString() ?: "Unknown error"
            throw Exception("tRPC error at $procedure: $msg")
        }

        val resultObj = first["result"]?.jsonObject
            ?: throw Exception("Missing 'result' in tRPC response from $procedure: ${responseBody.take(200)}")
        val data = resultObj["data"]
            ?: throw Exception("Missing 'result.data' in tRPC response from $procedure: ${responseBody.take(200)}")

        return data
    }
}
