package com.blesense.app.features.bluetooth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blesense.app.features.bluetooth.domain.usecase.*
import presentation.viewmodel.BluetoothScanViewModel

class BluetoothScanViewModelFactory(
    private val startBleScan: StartBleScanUseCase,
    private val stopBleScan: StopBleScanUseCase,
    private val observeScanningStatus: ObserveScanningStateUseCase,
    private val observeDevices: ObserveDevicesUseCase,
    private val clearDevices: ClearDevicesUseCase,
    private val observeLatestPacketId: ObserveLatestPacketIdUseCase,
    private val observeDataLoggerHistory: ObserveDataLoggerHistoryUseCase,
    private val observeTempLoggerHistory: ObserveTempLoggerHistoryUseCase,
    private val observeLatestTempLogger: ObserveLatestTempLoggerUseCase,
    private val addSensorPacket: AddSensorPacketUseCase,
    private val getDeviceHistory: GetDeviceHistoryUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BluetoothScanViewModel::class.java)) {
            return BluetoothScanViewModel(
                startBleScan = startBleScan,
                stopBleScan = stopBleScan,
                observeScanningStatus = observeScanningStatus,
                observeDevices = observeDevices,
                clearDevicesUseCase = clearDevices,
                observeLatestPacketId = observeLatestPacketId,
                observeDataLoggerHistory = observeDataLoggerHistory,
                observeTempLoggerHistory = observeTempLoggerHistory,
                observeLatestTempLogger = observeLatestTempLogger,
                addSensorPacket = addSensorPacket,
                getDeviceHistoryUseCase = getDeviceHistory

            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}