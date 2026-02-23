package presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.model.HistoricalDataEntry
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BluetoothScanViewModel(
    private val startBleScan: StartBleScanUseCase,
    private val stopBleScan: StopBleScanUseCase,
    private val observeScanningStatus: ObserveScanningStateUseCase,
    private val observeDevices: ObserveDevicesUseCase,
    private val clearDevicesUseCase: ClearDevicesUseCase,
    private val observeLatestPacketId: ObserveLatestPacketIdUseCase,
    private val observeDataLoggerHistory: ObserveDataLoggerHistoryUseCase,
    private val observeTempLoggerHistory: ObserveTempLoggerHistoryUseCase,
    private val observeLatestTempLogger: ObserveLatestTempLoggerUseCase,
    private val addSensorPacket: AddSensorPacketUseCase,
    private val getDeviceHistoryUseCase: GetDeviceHistoryUseCase,
    private val sendBleCommand: SendBleCommandUseCase,
    private val stopBleAdvertising: StopBleAdvertisingUseCase,
) : ViewModel() {

    /* ---------- STATE ---------- */

    val isScanning: StateFlow<Boolean> =
        observeScanningStatus()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val devices: StateFlow<List<BleDevice>> =
        observeDevices()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dataLoggerPacketHistory: StateFlow<List<SensorData.DataLoggerData>> =
        observeDataLoggerHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestPacketId: StateFlow<Int> =
        observeLatestPacketId()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    val tempLoggerPacketHistory: StateFlow<Map<String, List<SensorData.TempLoggerData>>> =
        observeTempLoggerHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val latestTempLoggerPacket: StateFlow<Map<String, SensorData.TempLoggerData?>> =
        observeLatestTempLogger()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /* ---------- ACTIONS ---------- */

    fun startScan() {
        startBleScan()
    }

    fun stopScan() {
        stopBleScan()
    }

    fun clearDevices() {
        clearDevicesUseCase()
    }

    fun onSensorDataReceived(data: SensorData) {
        viewModelScope.launch {
            addSensorPacket(data)
        }
    }

    fun observeTempLoggerHistory(deviceAddress: String): Flow<List<SensorData.TempLoggerData>> {
        return tempLoggerPacketHistory.map { it[deviceAddress] ?: emptyList() }
    }

    fun getDeviceHistory(deviceAddress: String): List<HistoricalDataEntry> {
        return getDeviceHistoryUseCase(deviceAddress)
    }

    fun requestDataLoggerDownload() {

        sendBleCommand(
            byteArrayOf(0xBB.toByte(), 0xCC.toByte()),
            40000
        )
    }

    fun requestReset() {

        sendBleCommand(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte()),
            40000
        )
    }

    fun stopAdvertising() {

        stopBleAdvertising()
    }
}
