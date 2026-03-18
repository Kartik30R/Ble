package com.blesense.app.core.network

import android.util.Log
import com.blesense.app.app.Routes
import com.blesense.app.features.bluetooth.data.entity.BlePacketUpload
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*

object BleWebSocketManager {

    private const val TAG = "BLE_WS"

    private val gson = Gson()

    private val client = OkHttpClient()

    private var socket: WebSocket? = null

    val isStreaming = MutableStateFlow(false)

    private const val WS_URL = "ws://${Routes.ip}/ws/ingest"

    fun connect() {

        if (socket != null) return

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        socket = client.newWebSocket(request, listener)

        Log.d(TAG, "WebSocket connecting")
    }

    fun disconnect() {

        socket?.close(1000, "closed")

        socket = null

        isStreaming.value = false

        Log.d(TAG, "WebSocket disconnected")
    }

    fun sendPacket(packet: BlePacketUpload) {

        if (!LiveStreamController.wsEnabled.value || !isStreaming.value) return
        val json = gson.toJson(packet)

        socket?.send(json)
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {

            Log.d(TAG, "WS connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {

            try {

                val cmd = gson.fromJson(text, CommandMessage::class.java)

                when (cmd.type) {

                    "start_stream" -> {

                        Log.d(TAG, "start_stream")

                        isStreaming.value = true
                    }

                    "stop_stream" -> {

                        Log.d(TAG, "stop_stream")

                        isStreaming.value = false
                    }
                }

            } catch (e: Exception) {

                Log.e(TAG, "Invalid WS message", e)
            }
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?
        ) {

            Log.e(TAG, "WS error", t)

            socket = null

            if (LiveStreamController.wsEnabled.value) {
                connect()
            }
        }
    }

    data class CommandMessage(
        val type: String,
        val deviceId: String?
    )
}