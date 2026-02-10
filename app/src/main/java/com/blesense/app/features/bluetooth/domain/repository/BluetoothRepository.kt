package com.blesense.app.features.bluetooth.domain.repository

import android.app.Activity
import com.blesense.app.features.bluetooth.domain.model.*
 import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    fun startScan(activity: Activity)
    fun stopScan()

    fun observeDevices(): Flow<List<BleDevice>>
    fun observeLatestPacketId(): Flow<Int>
    fun observeTempLoggerHistory(deviceAddress: String): Flow<List<SensorData.TempLoggerData>>
    fun getDeviceHistory(deviceAddress: String): List<HistoricalDataEntry>

    fun clearDevices()

    fun observeScanningState(): Flow<Boolean>

 }
