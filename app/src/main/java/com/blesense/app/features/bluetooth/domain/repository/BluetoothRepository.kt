package com.blesense.app.features.bluetooth.domain.repository

import com.blesense.app.features.bluetooth.domain.model.*
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {

    /* ---------- Scanner ---------- */

    fun startScan()

    fun stopScan()

    fun observeDevices(): Flow<List<BleDevice>>

    fun observeScanningState(): Flow<Boolean>

    fun clearDevices()


    /* ---------- Data observation ---------- */

    fun observeLatestPacketId(): Flow<Int>

    fun observeDataLoggerHistory(): Flow<List<SensorData.DataLoggerData>>

    fun observeTempLoggerHistory():
            Flow<Map<String, List<SensorData.TempLoggerData>>>

    fun observeLatestTempLogger():
            Flow<Map<String, SensorData.TempLoggerData?>>

    fun observeTempLoggerHistory(
        deviceAddress: String
    ): Flow<List<SensorData.TempLoggerData>>

    fun getDeviceHistory(
        deviceAddress: String
    ): List<HistoricalDataEntry>


    /* ---------- Data mutation ---------- */

    suspend fun addDataLoggerPacket(
        packet: SensorData.DataLoggerData
    )

    suspend fun addTempLoggerPacket(
        deviceAddress: String,
        packet: SensorData.TempLoggerData
    )


    /* ---------- Advertiser (NEW) ---------- */

    fun sendCommand(
        command: ByteArray,
        durationMs: Long = 5000
    )

    fun stopAdvertising()
}