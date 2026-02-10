package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData
import java.util.Locale

class AmmoniaParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 6) return null

        val deviceId = data[0].toUByte().toString()
        val ammonia = data[5].toUByte().toFloat()

        return SensorData.AmmoniaSensorData(
            deviceId = deviceId,
            ammonia = String.format(Locale.US, "%.1f ppm", ammonia),
            rawData = data.joinToString(" ") { "%02X".format(it) }
        )
    }
}
