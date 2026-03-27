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
    private val dataLogger: DataLoggerParser,
    private val sen6x: Sen6xParser
) {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun parse(result: ScanResult): SensorData? {

        val deviceName =
            result.scanRecord?.deviceName
                ?: result.device?.name
                ?: return null

        android.util.Log.d("BLE_NAME", "📡 Device detected: $deviceName")
        android.util.Log.d("BLE_NAME", "📡 Device address: ${result.device.address}")

        val manufacturerData =
            result.scanRecord?.manufacturerSpecificData
                ?: return null

        if (manufacturerData.size() == 0) return null

        val raw = manufacturerData.valueAt(0)
        val hex = raw.joinToString(" ") { String.format("%02X", it) }

        android.util.Log.d("BLE_RAW", "📦 Raw Data ($deviceName): $hex")

        if (manufacturerData.size() == 0) return null

        val deviceType = determineDeviceType(deviceName)

        android.util.Log.d("BLE_TYPE", "🧠 Detected Type: $deviceType for $deviceName")

        return when (deviceType) {
            "SHT40" -> sht40.parse(result)
            "Lux Sensor" -> lux.parse(result)
            "Soil Sensor" -> soil.parse(result)
            "LIS2DH" -> lis2dh.parse(result)
            "SPEED_DISTANCE" -> sdt.parse(result)
            "Ammonia Sensor" -> ammonia.parse(result)
            "DataLogger" -> dataLogger.parse(result)
            "TempLogger" -> tempLogger.parse(result)
            "SEN6x" -> sen6x.parse(result)
            else -> null
        }

    }

    private fun determineDeviceType(name: String?): String = when {
        name?.contains("SHT", ignoreCase = true) == true -> "SHT40"
        name?.contains("Lux_Data", ignoreCase = true) == true -> "Lux Sensor"
        name?.contains("SOIL", ignoreCase = true) == true -> "Soil Sensor"
        name?.contains("Activity", ignoreCase = true) == true -> "LIS2DH"
        name?.contains("Speed", ignoreCase = true) == true -> "SPEED_DISTANCE"
        name?.contains("NH", ignoreCase = true) == true -> "Ammonia Sensor"
        name?.contains("DataLogger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("Data Logger", ignoreCase = true) == true -> "DataLogger"
        name?.contains("DLOG", ignoreCase = true) == true -> "DataLogger"
        name?.contains("TempLogger", ignoreCase = true) == true -> "TempLogger"
        name?.contains("TLOG", ignoreCase = true) == true -> "TempLogger"
        name?.contains("Temp Logger", ignoreCase = true) == true -> "TempLogger"
        name?.contains("SEN", ignoreCase = true) == true -> "SEN6x"
        else -> "Unknown Device"
    }

}
