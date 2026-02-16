package com.blesense.app.features.bluetooth.data.datasource

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class AndroidBleScanner {

    private var callback: ScanCallback? = null
    private var restartJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _results = MutableSharedFlow<ScanResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val results: SharedFlow<ScanResult> = _results.asSharedFlow()

    /**
     * Starts BLE scanning.
     * Assumes permissions are already granted by UI layer.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        if (_isScanning.value) return

        val adapter = BluetoothAdapter.getDefaultAdapter()
        val scanner = adapter?.bluetoothLeScanner

        if (adapter == null || scanner == null || !adapter.isEnabled) {
            _isScanning.value = false
            return
        }

        _isScanning.value = true

        callback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                _results.tryEmit(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { _results.tryEmit(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

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
            } catch (_: Exception) {
                // Ignore hardware state errors
            }
        }

        callback = null
        _isScanning.value = false
    }

    /**
     * Android throttles long-running scans.
     * Restart every 5 minutes to maintain stability.
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
                    ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build(),
                    currentCallback
                )
            }
        }, 200)
    }
}
