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

    /* -------------------- USE CASES -------------------- */

    private fun startBleScanUseCase() =
        StartBleScanUseCase(repository())

    private fun stopBleScanUseCase() =
        StopBleScanUseCase(repository())

    private fun observeDevicesUseCase() =
        ObserveDevicesUseCase(repository())

    private fun observeLatestPacketIdUseCase() =
         ObserveLatestPacketIdUseCase(repository())

    private fun observeTempLoggerHistoryUseCase() =
         ObserveTempLoggerHistoryUseCase(repository())
    private fun clearDevicesUseCase() =
        ClearDevicesUseCase(repository())

    private fun getDeviceHistoryUseCase() =
         GetDeviceHistoryUseCase(repository())

    /* -------------------- VIEWMODEL FACTORY -------------------- */

    fun bluetoothScanViewModelFactory(): BluetoothScanViewModelFactory =
      BluetoothScanViewModelFactory(
            startBleScan = startBleScanUseCase(),
            stopBleScan = stopBleScanUseCase(),
            observeDevices = observeDevicesUseCase(),
            observeLatestPacketId = observeLatestPacketIdUseCase(),
            observeTempLoggerHistory = observeTempLoggerHistoryUseCase(),
          clearDevices = clearDevicesUseCase(),
            getDeviceHistory = getDeviceHistoryUseCase()
        )
}
