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

        val deviceName =
            result.scanRecord?.deviceName
                ?: result.device?.name
                ?: return null
        val manufacturerData = result.scanRecord?.manufacturerSpecificData
            ?: return null

        if (manufacturerData.size() == 0) return null

        val deviceType = when {
            deviceName.contains("SHT", true) -> "SHT40"
            deviceName.contains("Lux", true) -> "Lux"
            deviceName.contains("SOIL", true) -> "Soil"
            deviceName.contains("Activity", true) -> "LIS2DH"
            deviceName.contains("Speed", true) -> "SDT"
            deviceName.contains("NH", true) -> "Ammonia"
            deviceName.contains("DataLogger", true) ||
                    deviceName.contains("DLOG", true) -> "DataLogger"
            deviceName.contains("TempLogger", true) ||
                    deviceName.contains("TLOG", true) -> "TempLogger"
            else -> return null
        }

        return when (deviceType) {
            "SHT40" -> sht40.parse(result)
            "Lux" -> lux.parse(result)
            "Soil" -> soil.parse(result)
            "LIS2DH" -> lis2dh.parse(result)
            "SDT" -> sdt.parse(result)
            "Ammonia" -> ammonia.parse(result)
            "DataLogger" -> dataLogger.parse(result)
            "TempLogger" -> tempLogger.parse(result)
            else -> null
        }
    }



}
