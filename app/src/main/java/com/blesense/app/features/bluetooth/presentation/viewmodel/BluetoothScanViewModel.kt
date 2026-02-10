package presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.domain.usecase.*

import kotlinx.coroutines.flow.*
class BluetoothScanViewModel(
    private val startBleScan: StartBleScanUseCase,
    private val stopBleScan: StopBleScanUseCase,
    observeDevices: ObserveDevicesUseCase,
    observeLatestPacketId: ObserveLatestPacketIdUseCase,
    private val clearDevicesUseCase: ClearDevicesUseCase,
    private val observeTempLoggerHistory: ObserveTempLoggerHistoryUseCase,
    private val getDeviceHistory: GetDeviceHistoryUseCase,
    private val observeScanningStatus: ObserveScanningStateUseCase, // UseCase injected here
) : ViewModel() {

    /* -------------------- STATE FROM REPOSITORY -------------------- */

    val devices: StateFlow<List<BleDevice>> =
        observeDevices()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val latestPacketId: StateFlow<Int> =
        observeLatestPacketId()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                -1
            )

    /* -------------------- DYNAMIC UI STATE -------------------- */

    // FIX: Added () to invoke the Use Case and get the Flow
    val isScanning: StateFlow<Boolean> = observeScanningStatus()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    /* -------------------- ACTIONS -------------------- */

    fun startScan(activity: Activity) {
        // We no longer need to manually toggle a boolean!
        // The 'isScanning' flow above will update automatically
        // once the repository starts the hardware.
        startBleScan(activity)
    }

    fun stopScan() {
        stopBleScan()
    }

    fun observeTempLoggerHistory(deviceAddress: String): Flow<List<SensorData.TempLoggerData>> =
        observeTempLoggerHistory.invoke(deviceAddress)

    fun getFullDeviceHistory(deviceAddress: String) =
        getDeviceHistory(deviceAddress)

    fun clearDevices() {
        clearDevicesUseCase.invoke()
    }

    override fun onCleared() {
        stopBleScan()
        super.onCleared()
    }
}