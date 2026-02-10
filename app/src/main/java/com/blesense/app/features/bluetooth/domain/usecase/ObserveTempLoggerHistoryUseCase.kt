package com.blesense.app.features.bluetooth.domain.usecase


 import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class ObserveTempLoggerHistoryUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(deviceAddress: String): Flow<List<SensorData.TempLoggerData>> =
        repository.observeTempLoggerHistory(deviceAddress)
}
