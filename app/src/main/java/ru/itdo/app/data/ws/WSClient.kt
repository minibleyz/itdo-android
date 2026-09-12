package ru.itdo.app.data.ws

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.*
import ru.itdo.app.data.api.ItdoApi
import ru.itdo.app.data.model.Message
import ru.itdo.app.core.TokenStore
import java.util.concurrent.TimeUnit

/**
 * WebSocket-клиент для реального времени: сообщения, typing, онлайн статусы, звонки.
 * Подключается к WS серверу, авторизуется токеном, получает события.
 *
 * Полный аналог iOS WSClient.swift — обрабатывает все типы событий:
 * auth, typing, new_message, online_status, notification, incoming_call,
 * call_started, call_answered, call_signal, call_ended, call_error, pong.
 *
 * Автоматическое переподключение с экспоненциальной задержкой.
 * Использует OkHttp WebSocket (уже в зависимостях проекта).
 */
class WSClient(
    private val tokenStore: TokenStore,
    private val api: ItdoApi,
    private val gson: Gson
) {
    // ---- Состояние ----

    @Volatile var isConnected: Boolean = false
        private set

    /** Множество conv_id, где собеседник сейчас печатает. */
    val typingConvs: MutableSet<Int> = mutableSetOf()

    // ---- Обработчики событий (аналог iOS closures) ----

    /** (convId, message) — новое сообщение в диалоге. */
    var onNewMessage: ((Int, Message) -> Unit)? = null

    /** (convId, userId) — собеседник печатает. */
    var onTyping: ((Int, Int) -> Unit)? = null

    /** (userId, online) — изменение онлайн-статуса. */
    var onOnlineStatus: ((Int, Boolean) -> Unit)? = null

    /** (data) — входящее уведомление. */
    var onNotification: ((JsonObject) -> Unit)? = null

    /** (callId, callerName, type) — входящий звонок. */
    var onIncomingCall: ((Int, String, String) -> Unit)? = null

    /** (callId, existing) — звонок начат. */
    var onCallStarted: ((Int, Boolean) -> Unit)? = null

    /** (callId, response) — ответ на звонок ("accept"/"decline"). */
    var onCallAnswered: ((Int, String) -> Unit)? = null

    /** (callId, kind, payload) — сигнал звонка (offer/answer/candidate). */
    var onCallSignal: ((Int, String, JsonObject) -> Unit)? = null

    /** (callId, status) — звонок завершён. */
    var onCallEnded: ((Int, String) -> Unit)? = null

    /** (message) — ошибка звонка. */
    var onCallError: ((String) -> Unit)? = null

    // ---- Внутреннее состояние ----

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // без таймаута для WS
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectDelay: Long = 1000L
    private val maxReconnectDelay: Long = 30_000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("[WS] Connection opened")
            // Авторизуемся
            val token = kotlinx.coroutines.runBlocking { tokenStore.accessTokenOrNull() }
            if (token != null) {
                send(mapOf("action" to "auth", "token" to token))
            }
            // Пинг каждые 30 секунд
            pingJob = scope.launch {
                while (isActive) {
                    delay(30_000)
                    send(mapOf("action" to "ping"))
                }
            }
            reconnectDelay = 1000L
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleText(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
            handleText(bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("[WS] Connection closing: $code $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("[WS] Connection closed: $code $reason")
            handleDisconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("[WS] Connection failure: ${t.message}")
            handleDisconnect()
        }
    }

    // ================================================================
    //  ПОДКЛЮЧЕНИЕ / ОТКЛЮЧЕНИЕ
    // ================================================================

    /**
     * Подключиться к WebSocket серверу.
     * Сначала запрашивает URL через ws/info.php, затем подключается.
     * Если info.php недоступен, пробует стандартный порт 9502.
     */
    fun connect() {
        scope.launch {
            val token = tokenStore.accessTokenOrNull() ?: return@launch
            try {
                val info = api.fetchWsInfo()
                connectTo(info.url, token)
            } catch (e: Exception) {
                // Fallback: пробуем стандартный порт
                println("[WS] Failed to fetch ws/info.php: ${e.message}, trying fallback")
                // Формируем URL из базового адреса API
                val baseUrl = "wss://itdo.bleyzos.ru:9502"
                connectTo(baseUrl, token)
            }
        }
    }

    private fun connectTo(url: String, token: String) {
        println("[WS] Connecting to $url")
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = client.newWebSocket(request, listener)
    }

    /** Отключиться от WebSocket сервера. */
    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
    }

    /** Освободить ресурсы — вызывать при уничтожении приложения. */
    fun release() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }

    // ================================================================
    //  ОТПРАВКА СООБЩЕНИЙ
    // ================================================================

    /** Отправить произвольный словарь как JSON. */
    fun send(dict: Map<String, Any?>) {
        val json = gson.toJson(dict)
        webSocket?.send(json)
    }

    /** Сигнал "я печатаю" в диалоге. */
    fun sendTyping(convId: Int) {
        send(mapOf("action" to "typing", "conv_id" to convId))
    }

    // ================================================================
    //  СИГНАЛИНГ ЗВОНКОВ (аналог WS.send в веб-версии)
    // ================================================================

    /** Начать звонок. */
    fun sendCallStart(convId: Int, type: String) {
        send(mapOf("action" to "call_start", "conv_id" to convId, "type" to type))
    }

    /** Ответить на звонок (accept / decline). */
    fun sendCallAnswer(callId: Int, response: String) {
        send(mapOf("action" to "call_answer", "call_id" to callId, "action" to response))
    }

    /** Отправить сигнал звонка (offer / answer / candidate). */
    fun sendCallSignal(callId: Int, kind: String, payload: Map<String, Any?>) {
        send(mapOf("action" to "call_signal", "call_id" to callId, "kind" to kind, "payload" to payload))
    }

    /** Завершить звонок. */
    fun sendCallEnd(callId: Int) {
        send(mapOf("action" to "call_end", "call_id" to callId))
    }

    // ================================================================
    //  ОБРАБОТКА ВХОДЯЩИХ СООБЩЕНИЙ
    // ================================================================

    private fun handleText(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val action = json.get("action")?.asString ?: return

            when (action) {
                "auth_ok" -> {
                    isConnected = true
                    reconnectDelay = 1000L
                    val userId = json.get("user_id")?.asInt
                    println("[WS] Authenticated as user $userId")
                }

                "auth_error" -> {
                    val message = json.get("message")?.asString ?: "unknown"
                    println("[WS] Auth error: $message")
                    isConnected = false
                }

                "new_message" -> {
                    val convId = json.get("conv_id")?.asInt ?: return
                    val msgObj = json.getAsJsonObject("message") ?: return
                    try {
                        val message = gson.fromJson(msgObj, Message::class.java)
                        onNewMessage?.invoke(convId, message)
                    } catch (e: Exception) {
                        println("[WS] Failed to parse message: ${e.message}")
                    }
                }

                "typing" -> {
                    val convId = json.get("conv_id")?.asInt ?: return
                    val userId = json.get("user_id")?.asInt ?: return
                    typingConvs.add(convId)
                    onTyping?.invoke(convId, userId)
                    // Автоматически убираем через 2 секунды
                    scope.launch {
                        delay(2000)
                        typingConvs.remove(convId)
                    }
                }

                "online_status" -> {
                    val userId = json.get("user_id")?.asInt ?: return
                    val online = json.get("online")?.asBoolean ?: return
                    onOnlineStatus?.invoke(userId, online)
                }

                "notification" -> {
                    val data = json.getAsJsonObject("data") ?: return
                    onNotification?.invoke(data)
                }

                "incoming_call" -> {
                    val callId = json.get("call_id")?.asInt ?: return
                    val callerName = json.get("caller_name")?.asString ?: ""
                    val callType = json.get("call_type")?.asString ?: "audio"
                    onIncomingCall?.invoke(callId, callerName, callType)
                }

                "call_started" -> {
                    val callId = json.get("call_id")?.asInt ?: return
                    val existing = json.get("existing")?.asBoolean ?: false
                    onCallStarted?.invoke(callId, existing)
                }

                "call_answered" -> {
                    val callId = json.get("call_id")?.asInt ?: return
                    val response = json.get("response")?.asString ?: return
                    onCallAnswered?.invoke(callId, response)
                }

                "call_signal" -> {
                    val callId = json.get("call_id")?.asInt ?: return
                    val kind = json.get("kind")?.asString ?: return
                    val payload = json.getAsJsonObject("payload") ?: JsonObject()
                    onCallSignal?.invoke(callId, kind, payload)
                }

                "call_ended" -> {
                    val callId = json.get("call_id")?.asInt ?: return
                    val status = json.get("status")?.asString ?: "ended"
                    onCallEnded?.invoke(callId, status)
                }

                "call_error" -> {
                    val message = json.get("message")?.asString ?: "Ошибка звонка"
                    onCallError?.invoke(message)
                }

                "pong" -> {
                    // Ничего не делаем — просто подтверждение жизни соединения
                }
            }
        } catch (e: Exception) {
            println("[WS] Failed to parse message: ${e.message}")
        }
    }

    // ================================================================
    //  ПЕРЕПОДКЛЮЧЕНИЕ
    // ================================================================

    private fun handleDisconnect() {
        isConnected = false
        pingJob?.cancel()
        pingJob = null

        // Автоматическое переподключение с экспоненциальной задержкой
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            println("[WS] Reconnecting in ${reconnectDelay}ms...")
            delay(reconnectDelay)
            reconnectDelay = minOf(reconnectDelay * 2, maxReconnectDelay)
            connect()
        }
    }
}
