package com.blesense.app.core.permission

import android.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
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

    fun ensureReady(onReady: () -> Unit) {
        when {
            !hasPermissions() -> {
                _state.value = BluetoothPermissionState.PermissionsRequired
                permissionLauncher.launch(requiredPermissions())
            }
            !isBluetoothEnabled() -> {
                _state.value = BluetoothPermissionState.BluetoothDisabled
                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            !isLocationEnabled() -> {
                _state.value = BluetoothPermissionState.LocationDisabled
                locationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            else -> {
                _state.value = BluetoothPermissionState.Ready
                onReady()
            }
        }
    }

    fun onPermissionResult(granted: Boolean, onReady: () -> Unit) {
        if (!granted) {
            _state.value = BluetoothPermissionState.PermissionDenied
            return
        }
        ensureReady(onReady)
    }

    fun onBluetoothResult(onReady: () -> Unit) = ensureReady(onReady)
    fun onLocationResult(onReady: () -> Unit) = ensureReady(onReady)

    private fun hasPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        else
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

    private fun isBluetoothEnabled() =
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true

    private fun isLocationEnabled(): Boolean {
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
