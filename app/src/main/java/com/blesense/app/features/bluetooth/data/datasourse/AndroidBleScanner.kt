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

    private val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
    private var callback: ScanCallback? = null
    private var job: Job? = null

     private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _results = MutableSharedFlow<ScanResult>(extraBufferCapacity = 128)
    val results = _results.asSharedFlow()

    fun start(activity: Activity) {
        if (job != null || !hasPermission(activity)) return

         _isScanning.value = true

        job = CoroutineScope(Dispatchers.Default).launch {
            startInternal(activity)
            while (isActive) {
                delay(5 * 60 * 1000L)
                restart(activity)
            }
        }
    }

    fun stop() {
         _isScanning.value = false
        job?.cancel()
        job = null
        stopInternal()
    }
    @SuppressLint("MissingPermission")
    private fun startInternal(activity: Activity) {
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
             }
        }

        scanner?.startScan(
            null,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            callback
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopInternal() {
        callback?.let { scanner?.stopScan(it) }
        callback = null
    }

    private fun restart(activity: Activity) {
        stopInternal()
        Handler(Looper.getMainLooper()).postDelayed({
            startInternal(activity)
        }, 100)
    }

    private fun hasPermission(activity: Activity): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        else
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
