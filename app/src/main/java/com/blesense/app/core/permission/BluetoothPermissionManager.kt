package com.blesense.app.core.permission

import android.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.*

class BluetoothPermissionManager(
    private val activity: Activity,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val bluetoothLauncher: ActivityResultLauncher<Intent>,
    private val locationLauncher: ActivityResultLauncher<Intent>
) {

    private val _state = MutableStateFlow<BluetoothPermissionState>(BluetoothPermissionState.Idle)
    val state = _state.asStateFlow()

    /**
     * The core logic to check and request all requirements.
     */
    fun ensureReady(onReady: () -> Unit) {
        val permissions = requiredPermissions()
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            // 1. Check Permissions
            missingPermissions.isNotEmpty() -> {
                _state.value = BluetoothPermissionState.PermissionsRequired
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }

            // 2. Check Bluetooth Hardware State
            !isBluetoothEnabled() -> {
                _state.value = BluetoothPermissionState.BluetoothDisabled
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothLauncher.launch(enableBtIntent)
            }

            // 3. Check Location (Required for BLE discovery on most Android versions)
            !isLocationEnabled() -> {
                _state.value = BluetoothPermissionState.LocationDisabled
                val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationLauncher.launch(enableLocationIntent)
            }

            // 4. Everything is good
            else -> {
                _state.value = BluetoothPermissionState.Ready
                onReady()
            }
        }
    }

    /**
     * Called from the ActivityResultCallback in the UI layer
     */
    fun onPermissionResult(granted: Boolean, onReady: () -> Unit) {
        if (granted) {
            ensureReady(onReady)
        } else {
            _state.value = BluetoothPermissionState.PermissionDenied
        }
    }

    fun onBluetoothResult(onReady: () -> Unit) = ensureReady(onReady)
    fun onLocationResult(onReady: () -> Unit) = ensureReady(onReady)

    private fun isBluetoothEnabled(): Boolean {
        val bm = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bm.adapter?.isEnabled == true
    }

    private fun isLocationEnabled(): Boolean {
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION // Still needed for discovery unless flagged in Manifest
            )
        } else {
            // Android 6.0 to 11
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}