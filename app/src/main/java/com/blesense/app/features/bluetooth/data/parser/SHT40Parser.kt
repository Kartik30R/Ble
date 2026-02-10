package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class SHT40Parser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 5) return null


        val temp = data[1].toDouble() + (data[2].toUByte().toDouble() / 10000.0)
        val hum = data[3].toDouble() + (data[4].toUByte().toDouble() / 10000.0)

        return SensorData.SHT40Data(
            deviceId = data[0].toUByte().toString(),
            temperature = String.format("%.2f", temp),
            humidity = String.format("%.2f", hum)
        )
    }
}
