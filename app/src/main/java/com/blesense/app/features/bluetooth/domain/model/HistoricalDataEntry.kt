package com.blesense.app.features.bluetooth.domain.model

data class HistoricalDataEntry(
    val timestamp: Long,
    val sensorData: SensorData?
)
