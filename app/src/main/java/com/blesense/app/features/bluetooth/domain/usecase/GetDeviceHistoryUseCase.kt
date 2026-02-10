package com.blesense.app.features.bluetooth.domain.usecase

import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository


class GetDeviceHistoryUseCase(
    private val repository: BluetoothRepository
) {
    operator fun invoke(deviceAddress: String): List<HistoricalDataEntry> =
        repository.getDeviceHistory(deviceAddress)
}
