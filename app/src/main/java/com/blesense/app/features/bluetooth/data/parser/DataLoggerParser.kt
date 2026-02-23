package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class DataLoggerParser : SensorParser {

    private var dumpBaseTime: Long? = null
    override fun parse(result: ScanResult): SensorData? {

        val raw = result.scanRecord
            ?.manufacturerSpecificData
            ?.valueAt(0)
            ?: return null

        val requiredSize = 234

        val data = when {
            raw.size < requiredSize ->
                raw + ByteArray(requiredSize - raw.size)
            raw.size > requiredSize ->
                raw.copyOf(requiredSize)
            else -> raw
        }

        val deviceId = data[231].toInt() and 0xFF

        val accel = mutableListOf<Triple<Int,Int,Int>>()

        var i = 0
        while (i + 2 < 231) {
            accel.add(
                Triple(
                    data[i++].toInt() and 0xFF,
                    data[i++].toInt() and 0xFF,
                    data[i++].toInt() and 0xFF
                )
            )
        }

        return SensorData.DataLoggerData(
            deviceId = deviceId.toString(),
            currentPacketId = deviceId,
            lastPacketId = deviceId,
            payloadAccel = accel,
            timestamp = System.currentTimeMillis(),
            rawData = data.joinToString(" ") { "%02X".format(it) }
        )
    }
}
