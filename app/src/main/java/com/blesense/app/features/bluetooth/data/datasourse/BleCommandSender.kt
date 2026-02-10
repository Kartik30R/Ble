package com.blesense.app.features.bluetooth.data.datasourse

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class BleCommandSender(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser
    private val companyId = 0x0059 
    private var currentCallback: AdvertiseCallback? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun hasAdvertisePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun sendCommand(command: ByteArray, durationMs: Long = 5000) {
        if (!hasAdvertisePermission() || advertiser == null || command.size != 2) return
        stopAdvertising()

        val data = AdvertiseData.Builder()
            .addManufacturerData(companyId, command)
            .setIncludeDeviceName(false)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        currentCallback = object : AdvertiseCallback() {}
        
        try {
            advertiser.startAdvertising(settings, data, currentCallback)
            scope.launch {
                delay(durationMs)
                stopAdvertising()
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    fun stopAdvertising() {
        try {
            currentCallback?.let { advertiser?.stopAdvertising(it) }
            currentCallback = null
        } catch (e: SecurityException) { e.printStackTrace() }
    }
}