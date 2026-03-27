package com.blesense.app.core.di

import android.content.Context
import com.blesense.app.core.network.BleWebSocketManager
import com.blesense.app.core.network.RetrofitProvider
import com.blesense.app.features.bluetooth.data.datasource.AndroidBleScanner
import com.blesense.app.features.bluetooth.data.datasourse.InMemoryHistoryStore
import com.blesense.app.features.bluetooth.data.datasourse.BleCommandSender
import com.blesense.app.features.bluetooth.data.parser.*
import com.blesense.app.features.bluetooth.data.remote.BleRemoteDataSource
import com.blesense.app.features.bluetooth.data.repository.BluetoothRepositoryImpl
import com.blesense.app.features.bluetooth.domain.usecase.*
import com.blesense.app.features.bluetooth.presentation.viewmodel.BluetoothScanViewModelFactory

object BluetoothModule {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext

         BleWebSocketManager.connect(appContext)
    }
    /* ---------- Core dependencies ---------- */

    private val scanner by lazy {
        AndroidBleScanner(appContext)
    }

    private val historyStore by lazy {
        InMemoryHistoryStore()
    }

    private val remoteDataSource by lazy {
        BleRemoteDataSource(RetrofitProvider.api)
    }

    private val commandSender by lazy {
        BleCommandSender(appContext)
    }

    private val parserRouter by lazy {
        SensorParserRouter(
            lux = LuxParser(),
            ammonia = AmmoniaParser(),
            sht40 = SHT40Parser(),
            lis2dh = LIS2DHParser(),
            soil = SoilParser(),
            sdt = SDTParser(),
            tempLogger = TempLoggerParser(),
            dataLogger = DataLoggerParser(),
            sen6x = Sen6xParser()
        )
    }

    /* ---------- Repository ---------- */

    private val repository by lazy {
        val clientId = android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_mobile"
        BluetoothRepositoryImpl(
            scanner = scanner,
            parser = parserRouter,
            history = historyStore,
            commandSender = commandSender,
            remote = remoteDataSource,
            mobileId = clientId
        )
    }

    /* ---------- WebSocket ---------- */

    private val webSocketManager by lazy {
        BleWebSocketManager
    }

    /* ---------- Factory ---------- */

    fun bluetoothScanViewModelFactory(): BluetoothScanViewModelFactory {

        return BluetoothScanViewModelFactory(

            startBleScan = StartBleScanUseCase(repository),

            stopBleScan = StopBleScanUseCase(repository),

            observeScanningStatus =
                ObserveScanningStateUseCase(repository),

            observeDevices =
                ObserveDevicesUseCase(repository),

            clearDevices =
                ClearDevicesUseCase(repository),

            observeLatestPacketId =
                ObserveLatestPacketIdUseCase(repository),

            observeDataLoggerHistory =
                ObserveDataLoggerHistoryUseCase(repository),

            observeTempLoggerHistory =
                ObserveTempLoggerHistoryUseCase(repository),

            observeLatestTempLogger =
                ObserveLatestTempLoggerUseCase(repository),

            addSensorPacket =
                AddSensorPacketUseCase(repository),

            getDeviceHistory =
                GetDeviceHistoryUseCase(repository),

            sendBleCommand =
                SendBleCommandUseCase(repository),

            stopBleAdvertising =
                StopBleAdvertisingUseCase(repository),

            wsManager = webSocketManager
        )
    }
}