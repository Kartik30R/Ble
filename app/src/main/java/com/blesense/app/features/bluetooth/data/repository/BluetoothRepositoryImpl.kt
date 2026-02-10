package com.blesense.app.features.bluetooth.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import com.blesense.app.features.bluetooth.data.datasourse.AndroidBleScanner
import com.blesense.app.features.bluetooth.data.datasourse.InMemoryHistoryStore
import com.blesense.app.features.bluetooth.data.parser.SensorParserRouter
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.domain.repository.BluetoothRepository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@SuppressLint("MissingPermission")
class BluetoothRepositoryImpl(
    private val scanner: AndroidBleScanner,
    private val parser: SensorParserRouter,
    private val history: InMemoryHistoryStore
) : BluetoothRepository {

    private val devices = MutableStateFlow<List<BleDevice>>(emptyList())
    private val latestPacketId = MutableStateFlow(-1)

    init {

        val repoScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scanner.results.onEach { result ->
            val parsed = parser.parse(result) ?: return@onEach
            val deviceAddress = result.device.address


            val lastSavedEntry = history.get(deviceAddress).lastOrNull()?.sensorData

            val isNewData = when {
                parsed is SensorData.TempLoggerData && lastSavedEntry is SensorData.TempLoggerData -> {
                    parsed.rawData != lastSavedEntry.rawData
                }
                parsed is SensorData.DataLoggerData && lastSavedEntry is SensorData.DataLoggerData -> {
                    parsed.lastPacketId != lastSavedEntry.lastPacketId
                }

                else -> true
            }

            if (isNewData) {
                history.add(deviceAddress, HistoricalDataEntry(System.currentTimeMillis(), parsed))
            }

             val device = BleDevice(
                name = result.device.name ?: "Unknown",
                address = deviceAddress,
                rssi = result.rssi.toString(),
                deviceId = parsed.deviceId,
                sensorData = parsed
            )
            update(device)

             if (parsed is SensorData.DataLoggerData) {
                latestPacketId.value = parsed.currentPacketId
            }

        }.launchIn(repoScope)
    }
    override fun startScan(activity: Activity) = scanner.start(activity)
    override fun stopScan() = scanner.stop()

    override fun observeDevices() = devices
    override fun observeLatestPacketId() = latestPacketId

    override fun observeTempLoggerHistory(address: String): Flow<List<SensorData.TempLoggerData>> =
        flow {
            emit(history.get(address).mapNotNull { it.sensorData as? SensorData.TempLoggerData })
        }

    override fun getDeviceHistory(address: String) = history.get(address)

    private fun update(device: BleDevice) {
        devices.update {
            val i = it.indexOfFirst { d -> d.address == device.address }
            if (i >= 0) it.toMutableList().also { l -> l[i] = device }
            else it + device
        }
    }

    override fun clearDevices() {
         devices.value = emptyList()


        history.clearAll()
    }

    override fun observeScanningState(): Flow<Boolean> = scanner.isScanning
}
