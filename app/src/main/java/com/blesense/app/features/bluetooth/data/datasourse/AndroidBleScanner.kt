package com.blesense.app.features.bluetooth.data.datasourse

import android.*
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AndroidBleScanner {


    private var callback: ScanCallback? = null
    private var job: Job? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

     private val _results = MutableSharedFlow<ScanResult>(extraBufferCapacity = 128)
    val results = _results.asSharedFlow()

    /**
     * Entry point to start the BLE scan with automatic 5-minute restarts
     * to bypass Android's background scanning limitations.
     */
    fun start(activity: Activity) {
        if (job != null) return // Already running

        if (!hasPermission(activity)) {
            _isScanning.value = false
            return
        }

        _isScanning.value = true

        job = CoroutineScope(Dispatchers.Default).launch {
            startInternal(activity)
            // Loop for periodic restart logic
            while (isActive) {
                delay(5 * 60 * 1000L) // 5 minutes
                restart(activity)
            }
        }
    }

    /**
     * Stops the scan and cancels the background management job.
     */
    fun stop() {
        _isScanning.value = false
        job?.cancel()
        job = null
        stopInternal()
    }

    @SuppressLint("MissingPermission")
    private fun startInternal(activity: Activity) {
        // 1. Safety check for Bluetooth Hardware state
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val scanner = adapter?.bluetoothLeScanner

        if (adapter == null || scanner == null || !adapter.isEnabled) {
            _isScanning.value = false
            return
        }

        // 2. Define Callback
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!hasPermission(activity)) {
                    stop()
                    return
                }
                _results.tryEmit(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                if (!hasPermission(activity)) {
                    stop()
                    return
                }
                results.forEach { _results.tryEmit(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                // If scanning fails (too many scans, etc.), update state
                _isScanning.value = false
            }
        }

        // 3. Start Native Scan
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, callback)
    }

    @SuppressLint("MissingPermission")
    private fun stopInternal() {
        val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
        callback?.let {
            try {
                scanner?.stopScan(it)
            } catch (e: Exception) {
                // Handle cases where BT was turned off mid-scan
            }
        }
        callback = null
    }

    private fun restart(activity: Activity) {
        if (!isScanning.value) return

        stopInternal()
        // Brief delay before restarting to let the hardware reset
        Handler(Looper.getMainLooper()).postDelayed({
            if (job != null && job?.isActive == true) {
                startInternal(activity)
            }
        }, 200)
    }

    private fun hasPermission(activity: Activity): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
}