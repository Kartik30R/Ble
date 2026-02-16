package com.blesense.app.features.bluetooth.domain.usecase

import android.app.Activity
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class StartBleScanUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke() {
        repository.startScan()
    }
}
