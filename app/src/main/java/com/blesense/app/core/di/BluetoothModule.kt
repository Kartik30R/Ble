package com.blesense.app.core.di

import com.blesense.app.features.bluetooth.data.datasourse.AndroidBleScanner
import com.blesense.app.features.bluetooth.data.datasourse.InMemoryHistoryStore
 import com.blesense.app.features.bluetooth.data.parser.*
import com.blesense.app.features.bluetooth.data.repository.BluetoothRepositoryImpl
import com.blesense.app.features.bluetooth.domain.usecase.*
import com.blesense.app.features.bluetooth.presentation.viewmodel.BluetoothScanViewModelFactory

object BluetoothModule {

    /* -------------------- DATA LAYER -------------------- */

    private fun scanner() = AndroidBleScanner()

    private fun historyStore() = InMemoryHistoryStore()

    private fun parserRouter() = SensorParserRouter(
        lux =  LuxParser(),
        ammonia =  AmmoniaParser(),
        sht40 =  SHT40Parser(),
        lis2dh =  LIS2DHParser(),
        soil =  SoilParser(),
        sdt =  SDTParser(),
        tempLogger = TempLoggerParser(),
        dataLogger =  DataLoggerParser()
    )

    private fun repository() =
        BluetoothRepositoryImpl(
            scanner = scanner(),
            parser = parserRouter(),
            history = historyStore()
        )


    fun bluetoothScanViewModelFactory(): BluetoothScanViewModelFactory {
        return BluetoothScanViewModelFactory(
            startBleScan = StartBleScanUseCase(repository()),
            stopBleScan = StopBleScanUseCase(repository()),
            observeScanningStatus = ObserveScanningStateUseCase(repository()),
            observeDevices = ObserveDevicesUseCase(repository()),
            clearDevices = ClearDevicesUseCase(repository()),
            observeLatestPacketId = ObserveLatestPacketIdUseCase(repository()),
            observeDataLoggerHistory = ObserveDataLoggerHistoryUseCase(repository()),
            observeTempLoggerHistory = ObserveTempLoggerHistoryUseCase(repository()),
            observeLatestTempLogger = ObserveLatestTempLoggerUseCase(repository()),
            addSensorPacket = AddSensorPacketUseCase(repository()),
             getDeviceHistory = GetDeviceHistoryUseCase(repository()) // Pass this here
        )
    }
}