package com.blesense.app.features.bluetooth.data.datasourse

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.UUID

//
//class BleCommandSender(private val context: Context) {
//    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
//    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser
//    private val companyId = 0x004C
//    private var currentCallback: AdvertiseCallback? = null
//    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    private fun hasAdvertisePermission(): Boolean {
//        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
//        } else true
//    }
//
//    fun sendCommand(command: ByteArray, durationMs: Long = 5000) {
//
//        Log.d("BLE_ADV", "sendCommand called")
//
//        if (!hasAdvertisePermission()) {
//            Log.e("BLE_ADV", "No advertise permission")
//            return
//        }
//
//        if (advertiser == null) {
//            Log.e("BLE_ADV", "Advertiser is NULL")
//            return
//        }
//
//        Log.d("BLE_ADV", "Advertising command: ${command.joinToString()}")
//        if (!hasAdvertisePermission() || advertiser == null || command.size != 2) return
//        stopAdvertising()
//
//        val data = AdvertiseData.Builder()
//            .addManufacturerData(companyId, command)
//            .setIncludeDeviceName(false)
//            .build()
//
//        val settings = AdvertiseSettings.Builder()
//            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
//            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
//            .setConnectable(false)
//            .build()
//
//        currentCallback = object : AdvertiseCallback() {}
//
//        try {
//            advertiser.startAdvertising(settings, data, currentCallback)
//            scope.launch {
//                delay(durationMs)
//                stopAdvertising()
//            }
//        } catch (e: SecurityException) { e.printStackTrace() }
//    }
//
//    fun stopAdvertising() {
//        try {
//            currentCallback?.let { advertiser?.stopAdvertising(it) }
//            currentCallback = null
//        } catch (e: SecurityException) { e.printStackTrace() }
//    }
//}



@SuppressLint("MissingPermission")
class BleCommandSender(
    private val context: Context
) {
    private val companyId = 0x0059
    companion object {
        private const val TAG = "BLE_ADV"
        private val SERVICE_UUID =
            ParcelUuid(UUID.fromString("0000FEED-0000-1000-8000-00805F9B34FB"))
    }

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private val advertiser: BluetoothLeAdvertiser? =
        bluetoothAdapter?.bluetoothLeAdvertiser
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentCallback: AdvertiseCallback? = null
    private var advertisingJob: Job? = null
    
    private var lastCommandBytes: ByteArray? = null
    private var lastCompanyId: Int? = null

    init {
        Log.d(TAG, "BleCommandSender initialized")
        Log.d(TAG, "BluetoothAdapter = $bluetoothAdapter")
        Log.d(TAG, "Advertiser = $advertiser")
    }

    /* ------------------------------------------------ */
    /* Permission Check */
    /* ------------------------------------------------ */

    private fun hasAdvertisePermission(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Advertise permission granted = $granted")

            return granted
        }

        return true
    }

    /* ------------------------------------------------ */
    /* Send Command via BLE Advertising */
    /* ------------------------------------------------ */

    fun sendCommand(
        command: ByteArray,
        durationMs: Long = 5000,
        customCompanyId: Int? = null
    ) {
        Log.d(TAG, "Advertising STARTED successfully")

        Log.d(TAG, "sendCommand called")

        if (!hasAdvertisePermission()) {
            Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission")
            return
        }

        if (advertiser == null) {
            Log.e(TAG, "Device does NOT support BLE advertising")
            return
        }

        // Anti-flicker: If command is identical to current one, don't restart
        if (customCompanyId == lastCompanyId && command.contentEquals(lastCommandBytes)) {
            Log.d(TAG, "Identical command - skipping restart to prevent flicker")
            restartJob(durationMs) // Just refresh the timer
            return
        }

        stopAdvertising()

        lastCommandBytes = command
        lastCompanyId = customCompanyId ?: companyId

        Log.d(TAG, "Command bytes = ${command.joinToString()}")

        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                )
                .setTxPowerLevel(
                    AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                )
                .setConnectable(false)
                .setTimeout(0)
                .build()



        val data = AdvertiseData.Builder()
            .addManufacturerData(customCompanyId ?: companyId, command)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()


        currentCallback =
            object : AdvertiseCallback() {

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {

                    Log.d(TAG, "Advertising STARTED successfully")

                }

                override fun onStartFailure(errorCode: Int) {

                    Log.e(TAG, "Advertising FAILED: $errorCode")

                }
            }

        advertiser.startAdvertising(
            settings,
            data,
            currentCallback
        )

        Log.d(TAG, "startAdvertising called")
        
        /* Auto stop after duration */
        restartJob(durationMs)
    }

    private fun restartJob(durationMs: Long) {
        advertisingJob?.cancel()
        advertisingJob = scope.launch(Dispatchers.Default) {
            delay(durationMs)
            withContext(Dispatchers.Main) {
                stopAdvertising()
            }
        }
    }

    /* ------------------------------------------------ */
    /* Stop Advertising */
    /* ------------------------------------------------ */

    fun stopAdvertising() {
        if (!hasAdvertisePermission()) return
        if (advertiser == null) return
        
        advertisingJob?.cancel()
        advertisingJob = null
        
        lastCommandBytes = null
        lastCompanyId = null
        
        currentCallback?.let {
            advertiser.stopAdvertising(it)
            Log.d(TAG, "Advertising STOPPED")
            currentCallback = null
        }
    }


}