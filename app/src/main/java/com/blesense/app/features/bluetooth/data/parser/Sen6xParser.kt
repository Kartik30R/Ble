package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
import android.util.Log
import com.blesense.app.features.bluetooth.domain.model.SensorData

class Sen6xParser : SensorParser {

    override fun parse(result: ScanResult): SensorData? {
        val manufacturerData = result.scanRecord?.manufacturerSpecificData ?: return null

        var data: ByteArray? = null
        var startOffset = 0

        // The user's Arduino code uses 0x0059 as the manufacturer ID (Nordic)
        // mfg_data[0] = 0x59, mfg_data[1] = 0x00
        val nordicId = 0x0059

        for (i in 0 until manufacturerData.size()) {
            val key = manufacturerData.keyAt(i)
            val candidate = manufacturerData.valueAt(i)
            

            // Case 1: Android stripped the 2-byte ID (standard behavior)
            // mfg_data[2..20] = 19 bytes
            if (candidate.size == 19) {
                data = candidate
                startOffset = 2 // We are starting from mfg_data[2]
                 break
            }
            
            // Case 2: ID is included in the byte array for some reason
            // mfg_data[0..20] = 21 bytes
            if (candidate.size == 21) {
                data = candidate
                startOffset = 0 // We are starting from mfg_data[0]
                 break
            }
        }

        if (data == null) return null

        fun readUInt16(mfgIdx: Int): Int {
            val localIdx = mfgIdx - startOffset
            if (localIdx < 0 || localIdx + 1 >= (data?.size ?: 0)) return 0
            val msb = data!![localIdx].toUByte().toInt()
            val lsb = data!![localIdx + 1].toUByte().toInt()
            return (msb shl 8) or lsb
        }

        fun readInt16(mfgIdx: Int): Int {
            val localIdx = mfgIdx - startOffset
            if (localIdx < 0 || localIdx + 1 >= (data?.size ?: 0)) return 0
            val msb = data!![localIdx].toUByte().toInt()
            val lsb = data!![localIdx + 1].toUByte().toInt()
            return ((msb shl 8) or lsb).toShort().toInt()
        }

        fun readByte(mfgIdx: Int): Int {
            val localIdx = mfgIdx - startOffset
            if (localIdx < 0 || localIdx >= (data?.size ?: 0)) return 0
            return data!![localIdx].toUByte().toInt()
        }

        val deviceId = readByte(2).toString()
        val pm1 = readUInt16(3) / 10.0
        val pm25 = readUInt16(5) / 10.0
        val pm4 = readUInt16(7) / 10.0
        val pm10 = readUInt16(9) / 10.0
        val temp = readInt16(11) / 200.0
        val rh = readInt16(13) / 100.0
        val co2 = readUInt16(15)
        val voc = readInt16(17)
        val nox = readInt16(19)

        Log.d("SEN6X_PARSED", "ID:$deviceId PM2.5:$pm25 Temp:$temp CO2:$co2")

        return SensorData.Sen6xData(
            deviceId = deviceId,
            pm1 = "%.1f".format(pm1),
            pm25 = "%.1f".format(pm25),
            pm4 = "%.1f".format(pm4),
            pm10 = "%.1f".format(pm10),
            temperature = "%.2f".format(temp),
            humidity = "%.2f".format(rh),
            co2 = co2.toString(),
            voc = voc.toString(),
            nox = nox.toString()
        )
    }
}
