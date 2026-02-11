package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

class ObserveDataLoggerHistoryUseCase(private val repository: BluetoothRepository) {
    operator fun invoke() = repository.observeDataLoggerHistory()
}