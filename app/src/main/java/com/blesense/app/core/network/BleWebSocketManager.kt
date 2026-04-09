package com.blesense.app.core.network

import android.content.Context
import android.util.Log
import android.provider.Settings
import com.blesense.app.app.Routes
import com.blesense.app.features.bluetooth.data.entity.BlePacketUpload
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import okhttp3.*

object BleWebSocketManager {

    private const val TAG = "BLE_WS"

    private val gson = Gson()
    private var appContext: Context? = null
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 🔴 TWO SOCKETS
    private var ingestSocket: WebSocket? = null   // command socket

    val isStreaming = MutableStateFlow(false)
    private var currentMobileId: String? = null // Cache it

    // ✅ Track active device from command
    private var activeDeviceId: String? = null

    private fun getMobileId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_mobile"
    }

    private fun getIngestUrl(context: Context, token: String? = null): String {
        val baseUrl = "ws://${Routes.ip}/ws/ingest?clientId=${getMobileId(context)}"
        return if (token != null) "$baseUrl&token=$token" else baseUrl
    }

    private fun getStreamUrl(): String {
        return "ws://${Routes.ip}/ws"
    }

    fun connect(context: Context) {
        appContext = context.applicationContext
 
        if (ingestSocket != null) {
            Log.v(TAG, "Ingest WS already exists")
            return
        }
 
        scope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                val url = getIngestUrl(context, token)
                Log.i(TAG, "🔗 Connecting to Ingest WS (Authenticated: ${token != null})")
 
                val request = Request.Builder()
                    .url(url)
                    .build()
 
                ingestSocket = client.newWebSocket(request, ingestListener)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to connect WS: ${e.message}")
                // Retry after delay
                delay(5000)
                connect(context)
            }
        }
    }

    fun disconnect() {
        ingestSocket?.close(1000, "closed")


        ingestSocket = null


        activeDeviceId = null
        isStreaming.value = false

        Log.d(TAG, "All sockets disconnected")
    }

    // ✅ SEND ONLY filtered packets on stream socket
    fun sendPacket(packet: BlePacketUpload) {

        if (!isStreaming.value) return

        if (activeDeviceId != null) {
            val matches =
                (packet.deviceId == activeDeviceId) || (packet.deviceAddress == activeDeviceId)
            if (!matches) return

            if (!matches) {
                Log.v(
                    TAG,
                    "Filter: dropped packet from ${packet.deviceAddress} (active=$activeDeviceId)"
                )
                return
            }
        }
        val json = gson.toJson(packet)
        val success = ingestSocket?.send(json) ?: false
        if (!success) {
            Log.w(TAG, "Failed to send packet over stream socket")
        }
    }

    // =========================
    // 🔌 INGEST (COMMAND) SOCKET
    // =========================
    private val ingestListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Ingest WS connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val cmd = gson.fromJson(text, CommandMessage::class.java)

                when (cmd.type.lowercase()) {

                    "start_stream", "start_live" -> {
                        Log.i(TAG, "▶️ START_LIVE command received for device: ${cmd.deviceId}")
                        activeDeviceId = cmd.deviceId
                        startStreaming()
                    }

                    "stop_stream", "stop_live" -> {
                        Log.d(TAG, "STOP command for ${cmd.deviceId}")
                        if (cmd.deviceId == activeDeviceId) {
                            stopStreaming()
                            activeDeviceId = null
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling command: $text", e)
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "❌ Ingest WS error: ${t.message}. Reconnecting in 5s...", t)
            ingestSocket = null

            // Reconnect regardless of wsEnabled flag, as this is the command channel
            appContext?.let {
                scope.launch {
                    delay(5000)
                    connect(it)
                }
            }
        }
    }

    // =========================
    // 📡 STREAM SOCKET
    // =========================
    private fun startStreaming() {
        isStreaming.value = true
    }
    private fun stopStreaming() {

        isStreaming.value = false
    }

    data class CommandMessage(
        val type: String,
        val deviceId: String?
    )
}