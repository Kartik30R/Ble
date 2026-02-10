package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import com.blesense.app.features.bluetooth.domain.model.SensorData

class SDTParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val data = result.scanRecord?.manufacturerSpecificData?.valueAt(0) ?: return null
        if (data.size < 6) return null

        return SensorData.SDTData(
            deviceId = data[0].toUByte().toString(),
            speed = "${data[1].toUByte()}.${data[2].toUByte()}",
            distance = "${data[4].toUByte()}.${data[5].toUByte()}"
        )
    }
}
