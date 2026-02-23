package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class SendBleCommandUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(
        command: ByteArray,
        durationMs: Long = 5000
    ) {
        repository.sendCommand(command, durationMs)
    }
}