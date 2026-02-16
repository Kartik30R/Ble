package com.blesense.app.features.bluetooth.data.parser

import android.Manifest
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.blesense.app.features.bluetooth.domain.model.SensorData

class SensorParserRouter(
    private val lux: LuxParser,
    private val ammonia: AmmoniaParser,
    private val sht40: SHT40Parser,
    private val lis2dh: LIS2DHParser,
    private val soil: SoilParser,
    private val sdt: SDTParser,
    private val tempLogger: TempLoggerParser,
    private val dataLogger: DataLoggerParser
) {

     @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun parse(result: ScanResult): SensorData? {

        // Require manufacturer data like old code
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
            ?: return null

        if (manufacturerData.size() == 0) return null

         return lux.parse(result)
            ?: ammonia.parse(result)
            ?: sht40.parse(result)
            ?: lis2dh.parse(result)
            ?: soil.parse(result)
            ?: sdt.parse(result)
            ?: tempLogger.parse(result)
            ?: dataLogger.parse(result)
    }


}
