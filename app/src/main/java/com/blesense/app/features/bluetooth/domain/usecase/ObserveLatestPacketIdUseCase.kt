package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow

class ObserveLatestPacketIdUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<Int> =
        repository.observeLatestPacketId()
}
