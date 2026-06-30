package com.umbrel.android.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * tRPC HTTP transport envelope.
 *
 * tRPC uses JSON-RPC-like protocol over HTTP POST.
 * Body format: { "method": "query|mutation", "params": ["procedure.path", {...params}?] }
 */
@Serializable
data class TrpcEnvelope(
    val method: String,
    val params: List<JsonElement> = emptyList(),
)

/**
 * Error response from tRPC server.
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

/**
 * WebSocket server message from tRPC subscription.
 */
@Serializable
data class WsServerMessage(
    val id: Int? = null,
    val result: WsResult? = null,
)

@Serializable
data class WsResult(
    val data: JsonElement? = null,
)
