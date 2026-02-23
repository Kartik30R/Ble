package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class StopBleAdvertisingUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.stopAdvertising()
    }
}