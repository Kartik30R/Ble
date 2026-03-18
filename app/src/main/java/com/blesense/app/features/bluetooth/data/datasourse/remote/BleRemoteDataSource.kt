package com.blesense.app.features.bluetooth.data.remote

import android.util.Log
import com.blesense.app.core.network.BleWebSocketManager
import com.blesense.app.core.network.BleApiService
import com.blesense.app.features.bluetooth.data.entity.BlePacketUpload
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleRemoteDataSource @Inject constructor(
    private val api: BleApiService
) {

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

            Log.d(TAG, "🚀 Sending batch to server: size=$batchSize")

            val response = api.uploadBatch(batch)

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