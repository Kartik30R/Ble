package com.blesense.app.features.bluetooth.domain.model


sealed class SensorData {
    abstract val deviceId: String
    abstract fun toUploadMap(): Map<String, Any>

    data class SHT40Data(
        override val deviceId: String,
        val temperature: String,
        val humidity: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "SHT40",
            "temperature" to temperature,
            "humidity" to humidity
        )
    }

    data class LuxSensorData(
        override val deviceId: String,
        val lux: String,
        val rawData: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "LuxSensor",
            "lux" to lux,
            "rawData" to rawData
        )
    }

    data class LIS2DHData(
        override val deviceId: String,
        val x: String,
        val y: String,
        val z: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "LIS2DH",
            "x" to x,
            "y" to y,
            "z" to z
        )
    }

    data class SoilSensorData(
        override val deviceId: String,
        val nitrogen: String, val phosphorus: String, val potassium: String,
        val moisture: String, val temperature: String, val ec: String,
        val pH: String, val salinity: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "SoilSensor",
            "nitrogen" to nitrogen,
            "phosphorus" to phosphorus,
            "potassium" to potassium,
            "moisture" to moisture,
            "temperature" to temperature,
            "ec" to ec,
            "pH" to pH, // Matches Go: packet.Float("pH")
            "salinity" to salinity
        )
    }

    data class SDTData(
        override val deviceId: String,
        val speed: String,
        val distance: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "SpeedDistance", // Matches Go case: "SpeedDistance"
            "speed" to speed,
            "distance" to distance
        )
    }

    data class AmmoniaSensorData(
        override val deviceId: String,
        val ammonia: String,
        val rawData: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "AmmoniaSensor",
            "ammonia" to ammonia,
            "rawData" to rawData
        )
    }

    data class TempLoggerData(
        override val deviceId: String,
        val temperature: String,
        val humidity: String,
        val rawTemperature: Int,
        val rawHumidity: Int,
        val rawData: String,
        val deviceAddress: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "TempLogger",
            "temperature" to temperature,
            "humidity" to humidity,
            "rawTemperature" to rawTemperature,
            "rawHumidity" to rawHumidity,
            "rawData" to rawData,
            "deviceAddress" to deviceAddress
        )
    }

    data class DataLoggerData(
        override val deviceId: String,
        val currentPacketId: Int,
        val lastPacketId: Int,
        val payloadAccel: List<Triple<Int, Int, Int>>,
        val timestamp: Long,
        val rawData: String
    ) : SensorData() {
        override fun toUploadMap() = mapOf(
            "type" to "DataLogger",
            "currentPacketId" to currentPacketId,
            "lastPacketId" to lastPacketId,
            "payloadAccel" to payloadAccel, // Go uses packet.JSON("payloadAccel")
            "rawData" to rawData
        )
    }
}