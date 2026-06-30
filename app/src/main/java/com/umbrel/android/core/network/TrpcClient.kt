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
 * Kotlin-native tRPC HTTP client.
 *
 * tRPC v10 HTTP transport protocol (default):
 *   POST /trpc/{procedure}?batch=1
 *   Content-Type: application/json
 *   Body: JSON-serialized input (or {} for no input)
 *
 *   Response: [{ "result": { "data": <value> } }]  (always array, batch=1)
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

    /**
     * Execute raw tRPC HTTP call.
     *
     * tRPC v10 default protocol uses POST for both queries and mutations.
     *
     * Request:
     *   POST /trpc/{procedure}?batch=1
     *   Content-Type: application/json
     *   Body: JSON input (e.g. {} or {"password":"..."})
     *
     * Response:
     *   Status: 200
     *   Body:   [{ "result": { "data": <value> } }]
     *           or [{ "error": { "message": "...", "code": -32600 } }]
     */
    private suspend fun executeRaw(
        procedure: String,
        params: Map<String, JsonElement>?,
    ): JsonElement {
        val inputJson = if (params != null) {
            json.encodeToString(kotlinx.serialization.serializer(), params)
        } else {
            "{}"
        }

        val request = Request.Builder()
            .url("$baseUrl/trpc/$procedure?batch=1")
            .post(inputJson.toRequestBody("application/json".toMediaType()))
            .apply {
                authToken?.let { addHeader("Authorization", "Bearer $it") }
            }
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from server at $procedure")

        if (!response.isSuccessful) {
            val errorMsg = try {
                json.decodeFromString<TrpcErrorResponse>(responseBody).error.message
            } catch (_: Exception) { null }
            throw Exception(errorMsg ?: "HTTP ${response.code} from $procedure")
        }

        // tRPC wraps response in an array for batch=1: [{ "result": { "data": ... } }]
        val rootArray = try {
            json.parseToJsonElement(responseBody).jsonArray
        } catch (e: Exception) {
            throw Exception("Invalid tRPC response (not an array): ${responseBody.take(200)}")
        }

        if (rootArray.isEmpty()) {
            throw Exception("Empty response array from $procedure")
        }

        val first = rootArray[0].jsonObject

        // Check for tRPC error
        first["error"]?.let { errObj ->
            val msg = (errObj as? JsonObject)?.get("message")
            throw Exception("tRPC error: ${msg?.toString() ?: "Unknown error"}")
        }

        val resultObj = first["result"]?.jsonObject
            ?: throw Exception("tRPC response missing 'result' field: ${responseBody.take(200)}")
        val data = resultObj["data"]
            ?: throw Exception("tRPC response missing 'result.data' field: ${responseBody.take(200)}")

        return data
    }
}
