package com.blesense.app.features.bluetooth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blesense.app.features.bluetooth.domain.usecase.*
import presentation.viewmodel.BluetoothScanViewModel

class BluetoothScanViewModelFactory(
    private val startBleScan: StartBleScanUseCase,
    private val stopBleScan: StopBleScanUseCase,
    private val observeDevices: ObserveDevicesUseCase,
    private val observeLatestPacketId: ObserveLatestPacketIdUseCase,
    private val observeTempLoggerHistory: ObserveTempLoggerHistoryUseCase,
    private val clearDevices: ClearDevicesUseCase,
    private val getDeviceHistory: GetDeviceHistoryUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BluetoothScanViewModel(
                startBleScan,
                stopBleScan,
                observeDevices,
                observeLatestPacketId,
                clearDevices ,
                observeTempLoggerHistory,
                getDeviceHistory
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
