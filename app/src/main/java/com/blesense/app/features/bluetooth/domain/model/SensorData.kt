package com.blesense.app.features.bluetooth.domain.model


sealed class SensorData {
    abstract val deviceId: String

     data class SHT40Data(
        override val deviceId: String,
        val temperature: String,
        val humidity: String
    ) : SensorData()


    data class LuxSensorData(
        override val deviceId: String,
        val lux: String,
        val rawData: String
    ) : SensorData()

     data class LIS2DHData(
        override val deviceId: String,
        val x: String,
        val y: String,
        val z: String
    ) : SensorData()

     data class SoilSensorData(
        override val deviceId: String,
        val nitrogen: String,  // Nitrogen content
        val phosphorus: String,  // Phosphorus content
        val potassium: String,  // Potassium content
        val moisture: String,  // Soil moisture percentage
        val temperature: String,  // Soil temperature
        val ec: String,  // Electric conductivity
        val pH: String,  // Soil pH level
        val salinity: String  // Soil salinity
    ) : SensorData()

     data class SDTData(
        override val deviceId: String,
        val speed: String,
        val distance: String
    ) : SensorData()

     data class AmmoniaSensorData(
        override val deviceId: String,
        val ammonia: String,
        val rawData: String
    ) : SensorData()

     data class TempLoggerData(
        override val deviceId: String,
        val temperature: String,  // Formatted temperature
        val humidity: String,  // Formatted humidity
        val rawTemperature: Int,  // Raw integer temperature for calculations
        val rawHumidity: Int,  // Raw integer humidity for calculations
        val rawData: String,  // Raw hex data packet
        val deviceAddress: String,  // Device MAC address
        val timestamp: Long = System.currentTimeMillis()  // When data was received
    ) : SensorData() {

         val displaySummary: String
            get() = "Temp: $temperature°C, Hum: $humidity%, Device: $deviceId"
    }

     data class DataLoggerData(
        override val deviceId: String,
        val currentPacketId: Int,  // Total packets stored in device
        val lastPacketId: Int,  // Currently received packet ID
        val payloadAccel: List<Triple<Int, Int, Int>>,  // List of XYZ acceleration triplets
        val timestamp: Long,  // Calculated timestamp based on packet sequencing
        val rawData: String  // Raw hex data
    ) : SensorData() {

         val displaySummary: String
            get() = "Packet: $currentPacketId (last: $lastPacketId), Points: ${payloadAccel.size}, Time: $timestamp"
    }
}
