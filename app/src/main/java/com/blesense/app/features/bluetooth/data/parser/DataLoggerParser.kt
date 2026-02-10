package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class DataLoggerParser : SensorParser {

    private var dumpBaseTime: Long? = null

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 244) return null

        val size = data.size
        val lastPacketId =
            (data[size - 5].toInt() and 0xFF) or ((data[size - 4].toInt() and 0xFF) shl 8)
        val totalPackets =
            (data[size - 3].toInt() and 0xFF) or ((data[size - 2].toInt() and 0xFF) shl 8)

        if (dumpBaseTime == null) dumpBaseTime = System.currentTimeMillis()

        val packetAge = (totalPackets - lastPacketId) * 60_000L
        val timestamp = dumpBaseTime!! - packetAge

        val accel = mutableListOf<Triple<Int, Int, Int>>()
        var i = 0
        while (i + 2 < 240) {
            accel.add(Triple(data[i++].toInt(), data[i++].toInt(), data[i++].toInt()))
        }

        if (lastPacketId == 1) dumpBaseTime = null

        return SensorData.DataLoggerData(
            deviceId = "1",
            currentPacketId = totalPackets,
            lastPacketId = lastPacketId,
            payloadAccel = accel,
            timestamp = timestamp,
            rawData = data.joinToString(" ") { "%02X".format(it) }
        )
    }
}
