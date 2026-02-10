package com.blesense.app.features.bluetooth.domain.model

sealed class SensorData {
    abstract val deviceId: String

    data class LuxSensorData(override val deviceId: String, val lux: String, val rawData: String) : SensorData()
    data class AmmoniaSensorData(override val deviceId: String, val ammonia: String, val rawData: String) : SensorData()
    data class SHT40Data(override val deviceId: String, val temperature: String, val humidity: String) : SensorData()
    data class LIS2DHData(override val deviceId: String, val x: String, val y: String, val z: String) : SensorData()
    data class SoilSensorData(
        override val deviceId: String,
        val nitrogen: String,
        val phosphorus: String,
        val potassium: String,
        val moisture: String,
        val temperature: String,
        val ec: String,
        val pH: String,
        val salinity: String
    ) : SensorData()

    data class SDTData(override val deviceId: String, val speed: String, val distance: String) : SensorData()

    data class TempLoggerData(
        override val deviceId: String,
        val temperature: String,
        val humidity: String,
        val rawTemperature: Int,
        val rawHumidity: Int,
        val rawData: String,
        val deviceAddress: String,
        val timestamp: Long
    ) : SensorData()

    data class DataLoggerData(
        override val deviceId: String,
        val currentPacketId: Int,
        val lastPacketId: Int,
        val payloadAccel: List<Triple<Int, Int, Int>>,
        val timestamp: Long,
        val rawData: String
    ) : SensorData()
}
