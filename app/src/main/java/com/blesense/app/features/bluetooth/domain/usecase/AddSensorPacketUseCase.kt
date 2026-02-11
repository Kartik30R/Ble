package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class AddSensorPacketUseCase(private val repository: BluetoothRepository) {
    suspend operator fun invoke(sensorData: SensorData) {
        when (sensorData) {
            is SensorData.DataLoggerData -> repository.addDataLoggerPacket(sensorData)
            is SensorData.TempLoggerData -> repository.addTempLoggerPacket(sensorData.deviceAddress, sensorData)
             is SensorData.AmmoniaSensorData -> TODO()
            is SensorData.LIS2DHData -> TODO()
            is SensorData.LuxSensorData -> TODO()
            is SensorData.SDTData -> TODO()
            is SensorData.SHT40Data -> TODO()
            is SensorData.SoilSensorData -> TODO()
        }
    }
}