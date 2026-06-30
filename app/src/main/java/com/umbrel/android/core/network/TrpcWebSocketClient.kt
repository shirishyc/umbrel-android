package com.umbrel.android.core.network

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UmbrelOS tRPC event types that can be subscribed to over WebSocket.
 */
object UmbrelEvents {
    const val APPS_STATE_CHANGED = "apps:state-changed"
    const val HARDWARE_UPDATED = "hardware:updated"
    const val NOTIFICATIONS_NEW = "notifications:new"
    const val FILES_OPERATION_PROGRESS = "files:operation-progress"
    const val SYSTEM_UPDATE_STATUS = "system:update-status"

    val ALL = listOf(
        APPS_STATE_CHANGED,
        HARDWARE_UPDATED,
        NOTIFICATIONS_NEW,
        FILES_OPERATION_PROGRESS,
        SYSTEM_UPDATE_STATUS,
    )
}

/** WebSocket connection states */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

/**
 * Server message envelope from tRPC WebSocket subscription.
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

/**
 * WebSocket client for receiving real-time tRPC subscription updates from UmbrelOS.
 *
 * Manages connection lifecycle, reconnection with exponential backoff,
 * and exposes events via Kotlin Flow for ViewModels to collect.
 *
 * Protocol:
 *   Client -> Server: {"id":1,"method":"subscription","params":["eventBus.listen",{"event":"apps:state-changed"}]}
 *   Server -> Client: {"id":1,"result":{"data":{...}}}
 */
@Singleton
class TrpcWebSocketClient @Inject constructor(
    private val json: Json,
) {
    private var baseUrl: String = ""
    private var authToken: String? = null
    private var webSocket: WebSocket? = null
    private var shouldReconnect = false
    private val mutex = Mutex()

    // Maps subscription IDs -> event names for dedup
    private val activeSubscriptions = ConcurrentHashMap<Int, String>()
    private var nextId = 1

    // Dedicated OkHttp client for WebSocket (no read timeout for long-lived connection)
    private val wsClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    // Backpressure buffer for events from WebSocket
    private val _events = Channel<WsServerMessage>(Channel.UNLIMITED)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun configure(url: String, token: String?) {
        baseUrl = url.trimEnd('/')
        authToken = token
    }

    /** Connect and subscribe to all standard events */
    suspend fun connect() {
        mutex.withLock {
            if (webSocket != null) return
            shouldReconnect = true
            openSocket()
        }
    }

    private fun openSocket() {
        val token = authToken ?: return
        if (baseUrl.isBlank()) return

        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/') + "/trpc?token=$token"

        _connectionState.value = ConnectionState.CONNECTING

        webSocket = wsClient.newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _connectionState.value = ConnectionState.CONNECTED
                    // Re-subscribe active subscriptions from previous session
                    activeSubscriptions.forEach { (id, event) ->
                        sendSubscribe(id, event)
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = json.decodeFromString<WsServerMessage>(text)
                        _events.trySend(msg)
                    } catch (_: Exception) {
                        // Malformed message, skip
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    cleanup()
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    cleanup()
                    scheduleReconnect()
                }

                private fun cleanup() {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            },
        )
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        GlobalScope.launch {
            delay(3_000) // 3 second reconnect delay
            mutex.withLock {
                if (shouldReconnect && webSocket == null) {
                    openSocket()
                }
            }
        }
    }

    /** Subscribe to a specific event type. Returns subscription ID. */
    suspend fun subscribe(event: String): Int = mutex.withLock {
        val id = nextId++
        activeSubscriptions[id] = event
        sendSubscribe(id, event)
        id
    }

    /** Unsubscribe from an event */
    suspend fun unsubscribe(subscriptionId: Int) = mutex.withLock {
        activeSubscriptions.remove(subscriptionId)
        sendUnsubscribe(subscriptionId)
    }

    /** Subscribe to all standard events */
    suspend fun subscribeToAll() {
        for (event in UmbrelEvents.ALL) {
            subscribe(event)
        }
    }

    private fun sendSubscribe(id: Int, event: String) {
        val msg = buildJsonObject {
            put("id", id)
            put("method", "subscription")
            put("params", json.encodeToJsonElement(
                listOf(
                    "eventBus.listen",
                    buildJsonObject { put("event", event) },
                ),
            ))
        }
        webSocket?.send(json.encodeToString(serializer(), msg))
    }

    private fun sendUnsubscribe(id: Int) {
        val msg = buildJsonObject {
            put("id", id)
            put("method", "subscription.stop")
        }
        webSocket?.send(json.encodeToString(serializer(), msg))
    }

    /** Disconnect and stop reconnecting */
    suspend fun disconnect() = mutex.withLock {
        shouldReconnect = false
        webSocket?.close(1000, "Client closing")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Flow that emits decoded event data for a specific event type.
     * Automatically subscribes on collection and unsubscribes on cancellation.
     */
    inline fun <reified T> events(event: String): Flow<T> = callbackFlow {
        val id = subscribe(event)

        val collectorJob = GlobalScope.launch {
            for (msg in _events) {
                if (msg.id == id && msg.result?.data != null) {
                    try {
                        val data = json.decodeFromString<T>(msg.result.data.toString())
                        trySend(data)
                    } catch (_: Exception) {
                        // Type mismatch or malformed, skip
                    }
                }
            }
        }

        awaitClose {
            collectorJob.cancel()
            GlobalScope.launch { unsubscribe(id) }
        }
    }

    fun destroy() {
        runBlocking { disconnect() }
    }
}
