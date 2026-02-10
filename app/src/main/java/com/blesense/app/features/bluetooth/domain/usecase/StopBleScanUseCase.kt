package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class StopBleScanUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.stopScan()
    }
}
