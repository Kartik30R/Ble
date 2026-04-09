package com.blesense.app.features.bluetooth.data.remote

import android.util.Log
import com.blesense.app.core.network.BleWebSocketManager
import com.blesense.app.core.network.BleApiService
import com.blesense.app.features.bluetooth.data.entity.BlePacketUpload
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import javax.inject.Singleton

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

@Singleton
class BleRemoteDataSource @Inject constructor(
    private val api: BleApiService
) {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val TAG = "BLE_UPLOAD"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val packetChannel =
        Channel<BlePacketUpload>(capacity = 5000)
    private val batchBuffer = mutableListOf<BlePacketUpload>()

    private val BATCH_SIZE = 200
    private val FLUSH_INTERVAL = 5000L

    init {
        startWorker()
    }

     fun uploadPacket(packet: BlePacketUpload) {
        Log.d(TAG, "📤 Packet queued for upload: ID=${packet.deviceId}, Type=${packet.parsedType}")
        packetChannel.trySend(packet)
    }

    private fun startWorker() {
        scope.launch {
            Log.d(TAG, "👷 Remote Worker Started")
            var lastFlush = System.currentTimeMillis()

            for (packet in packetChannel) {

                // 🔌 WS → ONLY if streaming enabled
                if (BleWebSocketManager.isStreaming.value) {
                    try {
                        BleWebSocketManager.sendPacket(packet)
                    } catch (e: Exception) {
                        Log.w(TAG, "WS send failed: ${e.message}")
                    }
                }

                // 📦 HTTP → ALWAYS
                batchBuffer.add(packet)

                val now = System.currentTimeMillis()
                val shouldFlush =
                    batchBuffer.size >= BATCH_SIZE ||
                            (now - lastFlush > FLUSH_INTERVAL)

                if (shouldFlush) {
                    Log.d(TAG, "📦 Flushing batch (${batchBuffer.size}) to API...")
                    flush()
                    lastFlush = now
                }
            }
        }
    }

    private suspend fun flush() {
        if (batchBuffer.isEmpty()) return

        val batch = batchBuffer.toList()
        val batchSize = batch.size
        batchBuffer.clear()

        try {
            // 🛡️ Get Firebase Token (Anonymous or Email)
            val token = auth.currentUser?.getIdToken(true)?.await()?.token
            if (token == null) {
                Log.w(TAG, "⚠️ No Firebase token available, skipping batch upload")
                batchBuffer.addAll(batch)
                return
            }

            Log.d(TAG, "🚀 Sending batch to server: size=$batchSize (Authenticated)")

            val response = api.uploadBatch("Bearer $token", batch)

            Log.d(TAG, "📡 Response code: ${response.code()}")
            if (response.isSuccessful) {
                Log.i(TAG, "✅ Uploaded $batchSize packets")
            } else {
                Log.e(TAG, "❌ Upload failed: ${response.code()}")
                batchBuffer.addAll(batch) // restore packets
            }


        } catch (e: Exception) {
            Log.e(TAG, "🚨 Upload error FULL: ${e.message}", e)
            batchBuffer.addAll(batch) // restore packets
        }
    }
}