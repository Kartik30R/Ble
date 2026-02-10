package com.blesense.app.features.bluetooth.data.parser

import android.bluetooth.le.ScanResult
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

    fun parse(result: ScanResult): SensorData? {
        val name = result.device.name ?: return null

        return when {
            name.contains("Lux_Data", true) -> lux.parse(result)
            name.contains("NH", true) -> ammonia.parse(result)
            name.contains("SHT", true) -> sht40.parse(result)
            name.contains("Activity", true) -> lis2dh.parse(result)
            name.contains("SOIL", true) -> soil.parse(result)
            name.contains("Speed", true) -> sdt.parse(result)
            name.contains("TempLogger", true) ||
            name.contains("TLOG", true) ||
            name.contains("Temp Logger", true) -> tempLogger.parse(result)

            name.contains("DataLogger", true) ||
            name.contains("DLOG", true) ||
            name.contains("Data Logger", true) -> dataLogger.parse(result)

            else -> null
        }
    }
}
