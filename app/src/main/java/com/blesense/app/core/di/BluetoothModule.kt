package com.blesense.app.core.di

 import com.blesense.app.features.bluetooth.data.datasourse.InMemoryHistoryStore
 import com.blesense.app.features.bluetooth.data.parser.*
import com.blesense.app.features.bluetooth.data.repository.BluetoothRepositoryImpl
import com.blesense.app.features.bluetooth.domain.usecase.*
import com.blesense.app.features.bluetooth.presentation.viewmodel.BluetoothScanViewModelFactory
 import com.blesense.app.features.bluetooth.data.datasource.AndroidBleScanner

object BluetoothModule {

    private lateinit var appContext: android.content.Context

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private val scanner by lazy { AndroidBleScanner(appContext) }
    private val historyStore by lazy { InMemoryHistoryStore() }
    private val parserRouter by lazy {
        SensorParserRouter(
            lux = LuxParser(), ammonia = AmmoniaParser(),
            sht40 = SHT40Parser(), lis2dh = LIS2DHParser(),
            soil = SoilParser(), sdt = SDTParser(),
            tempLogger = TempLoggerParser(), dataLogger = DataLoggerParser()
        )
    }

    private val repository by lazy {
        BluetoothRepositoryImpl(
            scanner = scanner,
            parser = parserRouter,
            history = historyStore
        )
    }

    fun bluetoothScanViewModelFactory(): BluetoothScanViewModelFactory {
        return BluetoothScanViewModelFactory(
            startBleScan = StartBleScanUseCase(repository),
            stopBleScan = StopBleScanUseCase(repository),
            observeScanningStatus = ObserveScanningStateUseCase(repository),
            observeDevices = ObserveDevicesUseCase(repository),
            clearDevices = ClearDevicesUseCase(repository),
            observeLatestPacketId = ObserveLatestPacketIdUseCase(repository),
            observeDataLoggerHistory = ObserveDataLoggerHistoryUseCase(repository),
            observeTempLoggerHistory = ObserveTempLoggerHistoryUseCase(repository),
            observeLatestTempLogger = ObserveLatestTempLoggerUseCase(repository),
            addSensorPacket = AddSensorPacketUseCase(repository),
             getDeviceHistory = GetDeviceHistoryUseCase(repository)
        )
    }
}