package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class TempLoggerParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 5) return null
        val deviceId = data[0].toInt() and 0xFF
        val tInt = data[1].toInt() and 0xFF
        val tFrac = data[2].toInt() and 0xFF
        val hInt = data[3].toInt() and 0xFF
        val hFrac = data[4].toInt() and 0xFF

        val temp = "${data[1]}.${data[2].toUByte()}".toDouble()
        val hum = "${data[3]}.${data[4].toUByte()}".toDouble()

         if (temp !in 5.0..60.0 || hum !in 10.0..99.0) return null


        return SensorData.TempLoggerData(
            deviceId = deviceId.toString(),
            temperature = temp.toString(),
            humidity = hum.toString(),
            rawTemperature = data[1] * 100 + data[2],
            rawHumidity = data[3] * 100 + data[4],
            rawData = data.joinToString(" ") { "%02X".format(it) },
            deviceAddress = result.device.address,
            timestamp = System.currentTimeMillis()
        )
    }
}
