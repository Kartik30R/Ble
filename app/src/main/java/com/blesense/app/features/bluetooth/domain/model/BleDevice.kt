package com.blesense.app.features.bluetooth.domain.model


data class BleDevice(
    val name: String,
    val address: String,
    val rssi: String,
    val deviceId: String,
    val sensorData: SensorData?
)
