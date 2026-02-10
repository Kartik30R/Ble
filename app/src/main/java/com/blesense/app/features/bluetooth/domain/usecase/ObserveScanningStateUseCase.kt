package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class ObserveScanningStateUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeScanningState()
}