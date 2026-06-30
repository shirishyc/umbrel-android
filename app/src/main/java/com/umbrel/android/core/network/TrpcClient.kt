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
 * tRPC v10 HTTP transport protocol:
 *   - Query:  GET  /trpc/{procedure}?batch=1&input={}
 *   - Mutation: POST /trpc/{procedure} with body = JSON input
 *   - Response: [{ "result": { "data": ... } }]  (array, for batch=1)
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
        val data = executeRaw("system.status", null, isQuery = true)
        data is JsonObject
    }

    suspend fun <T> query(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute(procedure, params, isQuery = true, deserializer)

    suspend fun <T> mutation(
        procedure: String,
        params: Map<String, JsonElement>? = null,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = execute(procedure, params, isQuery = false, deserializer)

    private suspend fun <T> execute(
        procedure: String,
        params: Map<String, JsonElement>?,
        isQuery: Boolean,
        deserializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> = runCatching {
        val data = executeRaw(procedure, params, isQuery)
        json.decodeFromJsonElement(deserializer, data)
    }

    /**
     * Execute raw tRPC HTTP call and return result.data as JsonElement.
     *
     * tRPC v10 protocol:
     *   GET  /trpc/{path}?batch=1&input=<json>   for queries
     *   POST /trpc/{path}?batch=1                for mutations (body = input)
     *   Response: [{ "result": { "data": <value> } }]
     */
    private suspend fun executeRaw(
        procedure: String,
        params: Map<String, JsonElement>?,
        isQuery: Boolean,
    ): JsonElement {
        val inputJson = if (params != null) {
            json.encodeToString(kotlinx.serialization.serializer(), params)
        } else {
            "{}"
        }

        val requestBuilder = Request.Builder()

        if (isQuery) {
            // tRPC queries use GET
            requestBuilder
                .url("$baseUrl/trpc/$procedure?batch=1&input=$inputJson")
                .get()
        } else {
            // tRPC mutations use POST
            requestBuilder
                .url("$baseUrl/trpc/$procedure?batch=1")
                .post(inputJson.toRequestBody("application/json".toMediaType()))
        }

        authToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from server at $procedure")

        if (!response.isSuccessful) {
            // Try to parse tRPC error from response body
            val errorMsg = try {
                val err = json.decodeFromString<TrpcErrorResponse>(responseBody)
                err.error.message
            } catch (_: Exception) {
                null
            }
            throw Exception(errorMsg ?: "HTTP ${response.code} from $procedure")
        }

        // tRPC wraps response in array: [{ "result": { "data": <value> } }]
        val rootArray = json.parseToJsonElement(responseBody).jsonArray
        if (rootArray.isEmpty()) {
            throw Exception("Empty response array from $procedure")
        }
        val first = rootArray[0].jsonObject
        val resultObj = first["result"]?.jsonObject
            ?: throw Exception("tRPC error at $procedure: missing 'result' in response")
        val data = resultObj["data"]
            ?: throw Exception("tRPC error at $procedure: missing 'result.data' in response")

        return data
    }
}
