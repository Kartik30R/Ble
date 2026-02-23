package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import android.content.ContentValues.TAG
import android.util.Log
import com.blesense.app.features.bluetooth.domain.model.SensorData

class SoilParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {

        val data = result.scanRecord
            ?.manufacturerSpecificData
            ?.valueAt(0) ?: return null
        for (i in data.indices) {
            val u = data[i].toUByte().toInt()
            Log.d(TAG, "Index $i = $u (hex: ${String.format("%02X", u)})")
        }
        if (data.size < 16) return null
        val ecValue = (data[10].toUByte().toInt() or
                (data[11].toUByte().toInt() shl 8))
        return SensorData.SoilSensorData(

            deviceId = data[0].toUByte().toString(),

            nitrogen = data[1].toUByte().toString(),

            phosphorus = data[3].toUByte().toString(),

            potassium = data[5].toUByte().toString(),

            moisture = "${data[7].toUByte()}",

             temperature = "${data[8].toUByte()}.${data[9].toUByte()}",


             ec = ecValue.toString(),

             pH = "${data[12].toUByte()}.${data[13].toUByte()}",

             salinity = data[14].toUByte().toString()
        )
    }
}