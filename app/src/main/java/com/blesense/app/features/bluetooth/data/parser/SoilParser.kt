package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class SoilParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 13) return null

        return SensorData.SoilSensorData(
            deviceId = data[0].toUByte().toString(),
            nitrogen = data[1].toUByte().toString(),
            phosphorus = data[2].toUByte().toString(),
            potassium = data[3].toUByte().toString(),
            moisture = data[4].toUByte().toString(),
            temperature = "${data[5].toUByte()}.${data[6].toUByte()}",
            ec = "${data[7].toUByte()}.${data[8].toUByte()}",
            pH = "${data[9].toUByte()}.${data[10].toUByte()}",
            salinity = ((data[11].toUByte().toInt() shl 8) or data[12].toUByte().toInt()).toString()
        )
    }
}
