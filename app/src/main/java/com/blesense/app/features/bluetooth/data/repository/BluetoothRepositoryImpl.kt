package com.blesense.app.features.bluetooth.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import com.blesense.app.features.bluetooth.data.datasource.AndroidBleScanner
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

    // Main Device List and Status
    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    private val _latestPacketId = MutableStateFlow(-1)

    // Specialized History Flows
    private val _dataLoggerHistory = MutableStateFlow<List<SensorData.DataLoggerData>>(emptyList())
    private val _tempLoggerHistory = MutableStateFlow<Map<String, List<SensorData.TempLoggerData>>>(emptyMap())
    private val _latestTempLogger = MutableStateFlow<Map<String, SensorData.TempLoggerData?>>(emptyMap())

    private val repoScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

     init {
        scanner.results
            .onEach { result ->

                val deviceAddress = result.device.address
                val parsed = parser.parse(result)

                if (parsed != null) {

                    val device = BleDevice(
                        name = result.scanRecord?.deviceName
                            ?: result.device.name
                            ?: "BLE Device",
                        address = deviceAddress,
                        rssi = result.rssi.toString(),
                        deviceId = parsed.deviceId,
                        sensorData = parsed
                    )

                    updateDeviceList(device)

                     history.add(
                        deviceAddress,
                        HistoricalDataEntry(
                            timestamp = System.currentTimeMillis(),
                            sensorData = parsed
                        )
                    )

                    when (parsed) {

                        is SensorData.DataLoggerData -> {
                            _latestPacketId.value = parsed.currentPacketId
                            repoScope.launch {
                                addDataLoggerPacket(parsed)
                            }
                        }

                        is SensorData.TempLoggerData -> {
                            repoScope.launch {
                                addTempLoggerPacket(deviceAddress, parsed)
                            }
                        }

                        else -> Unit
                    }
                }
            }
            .launchIn(repoScope)
    }

    /* ---------- Scan Controls ---------- */

    override fun startScan( ) = scanner.start( )
    override fun stopScan() = scanner.stop()
    override fun observeScanningState(): Flow<Boolean> = scanner.isScanning

    /* ---------- Data Observation ---------- */

    override fun observeDevices() = _devices.asStateFlow()
    override fun observeLatestPacketId() = _latestPacketId.asStateFlow()
    override fun observeDataLoggerHistory() = _dataLoggerHistory.asStateFlow()
    override fun observeTempLoggerHistory() = _tempLoggerHistory.asStateFlow()
    override fun observeLatestTempLogger() = _latestTempLogger.asStateFlow()

    // Specific fetch for history
    override fun getDeviceHistory(address: String) = history.get(address)

    // Legacy/Manual history fetch
    override fun observeTempLoggerHistory(address: String): Flow<List<SensorData.TempLoggerData>> =
        _tempLoggerHistory.map { it[address] ?: emptyList() }

    /* ---------- State Mutation ---------- */

    private fun updateDeviceList(device: BleDevice) {
        _devices.update { currentList ->
            val index = currentList.indexOfFirst { it.address == device.address }
            if (index >= 0) {
                currentList.toMutableList().apply { this[index] = device }
            } else {
                currentList + device
            }
        }
    }

    override suspend fun addDataLoggerPacket(packet: SensorData.DataLoggerData) {
        _dataLoggerHistory.update { current ->
            // Deduplicate based on Packet ID
            if (current.any { it.lastPacketId == packet.lastPacketId }) current
            else current + packet
        }
    }

    override suspend fun addTempLoggerPacket(deviceAddress: String, packet: SensorData.TempLoggerData) {
        // Update the "Latest" snapshot map
        _latestTempLogger.update { it + (deviceAddress to packet) }

        // Update the history map
        _tempLoggerHistory.update { currentMap ->
            val deviceList = currentMap[deviceAddress] ?: emptyList()
            // Deduplicate based on raw byte data
            if (deviceList.any { it.rawData == packet.rawData }) {
                currentMap
            } else {
                currentMap + (deviceAddress to (deviceList + packet))
            }
        }
    }

    override fun clearDevices() {
        _devices.value = emptyList()
        _dataLoggerHistory.value = emptyList()
        _tempLoggerHistory.value = emptyMap()
        _latestTempLogger.value = emptyMap()
        _latestPacketId.value = -1
        history.clearAll()
    }
}