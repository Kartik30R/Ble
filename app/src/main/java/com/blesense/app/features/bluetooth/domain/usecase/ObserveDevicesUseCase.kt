package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class ObserveDevicesUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<List<BleDevice>> =
        repository.observeDevices()
}
