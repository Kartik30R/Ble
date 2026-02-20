package com.blesense.app.features.bluetooth.data.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class AndroidBleScanner(private val context: Context) {

    private var callback: ScanCallback? = null
    private var restartJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _results = MutableSharedFlow<ScanResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val results: SharedFlow<ScanResult> = _results.asSharedFlow()

    /**
     * Starts BLE scanning with full BLE 5 extended support.
     */
    @SuppressLint("MissingPermission")
    fun start() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                _isScanning.value = false
                return
            }
        }

        if (_isScanning.value) return

        val bluetoothManager =
            context.getSystemService(BluetoothManager::class.java)

        val adapter = bluetoothManager?.adapter
        val scanner = adapter?.bluetoothLeScanner

        if (adapter == null || scanner == null || !adapter.isEnabled) {
            _isScanning.value = false
            return
        }

        _isScanning.value = true

        callback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {

                Log.d("SCAN_DEBUG", "---- RAW SCAN ----")
                Log.d("SCAN_DEBUG", "Device: ${result.device.address}")
                Log.d("SCAN_DEBUG", "DeviceName: ${result.device.name}")
                Log.d("SCAN_DEBUG", "AdvName: ${result.scanRecord?.deviceName}")
                Log.d("SCAN_DEBUG", "ScanRecord: ${result.scanRecord}")
                Log.d(
                    "SCAN_DEBUG",
                    "ManufacturerData size: ${result.scanRecord?.manufacturerSpecificData?.size()}"
                )

                scope.launch {
                    _results.emit(result)
                }
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { _results.tryEmit(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
            }
        }

        val settings = createExtendedScanSettings()

        scanner.startScan(null, settings, callback)

        startPeriodicRestart()


    }

    /**
     * Stops BLE scanning completely.
     */
    @SuppressLint("MissingPermission")
    fun stop() {
        if (!_isScanning.value) return

        restartJob?.cancel()
        restartJob = null

        val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

        callback?.let {
            try {
                scanner?.stopScan(it)
            } catch (_: Exception) {}
        }

        callback = null
        _isScanning.value = false
    }

    /**
     * Restart scan every 5 minutes to prevent Android throttling.
     */
    private fun startPeriodicRestart() {
        restartJob?.cancel()

        restartJob = scope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L)
                restart()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restart() {
        if (!_isScanning.value) return

        val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
        val currentCallback = callback ?: return

        try {
            scanner?.stopScan(currentCallback)
        } catch (_: Exception) {}

        Handler(Looper.getMainLooper()).postDelayed({
            if (_isScanning.value) {
                scanner?.startScan(
                    null,
                    createExtendedScanSettings(),
                    currentCallback
                )
            }
        }, 200)
    }

    /**
     * Full BLE 5 extended scan configuration.
     */
    private fun createExtendedScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .build()
    }
}