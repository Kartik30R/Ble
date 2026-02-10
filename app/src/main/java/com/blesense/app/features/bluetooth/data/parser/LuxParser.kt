package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData
import androidx.core.util.size

class LuxParser : SensorParser {
    override fun parse(result: ScanResult): SensorData? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return null

         for (i in 0 until manufacturerData.size) {
            val data = manufacturerData.valueAt(i)
            if (data != null && data.size >= 3) {
                val deviceId = data[0].toInt() and 0xFF
                val highLux = data[1].toInt() and 0xFF
                val lowLux = data[2].toInt() and 0xFF
                val luxValue = (highLux * 256) + lowLux

                return SensorData.LuxSensorData(
                    deviceId = deviceId.toString(),
                    lux = luxValue.toString(),
                    rawData = data.joinToString(" ") { "%02X".format(it) }
                )
            }
        }
        return null
    }
}
