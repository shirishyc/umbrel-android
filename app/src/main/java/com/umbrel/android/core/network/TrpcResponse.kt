package com.umbrel.android.core.network

import kotlinx.serialization.Serializable

/**
 * tRPC HTTP transport envelope.
 *
 * tRPC uses JSON-RPC-like protocol over HTTP POST.
 * The body format is:
 *   { "method": "query"|"mutation", "params": ["procedure.path", {...params}?] }
 */
@Serializable
data class TrpcEnvelope(
    val method: String, // "query" or "mutation"
    val params: List<kotlinx.serialization.json.JsonElement> = emptyList(),
)

/**
 * Successful tRPC response wrapper.
 */
@Serializable
data class TrpcSuccess<T>(
    val result: TrpcResult<T>,
)

@Serializable
data class TrpcResult<T>(
    val data: T,
)

/**
 * Error tRPC response.
 */
@Serializable
data class TrpcErrorResponse(
    val error: TrpcErrorDetail,
)

@Serializable
data class TrpcErrorDetail(
    val message: String? = null,
    val code: Int = -1,
)
