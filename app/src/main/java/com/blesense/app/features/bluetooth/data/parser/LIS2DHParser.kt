package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class LIS2DHParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 7) return null

        return SensorData.LIS2DHData(
            deviceId = data[0].toUByte().toString(),
            x = "${data[1]}.${data[2].toUByte()}",
            y = "${data[3]}.${data[4].toUByte()}",
            z = "${data[5]}.${data[6].toUByte()}"
        )
    }
}
