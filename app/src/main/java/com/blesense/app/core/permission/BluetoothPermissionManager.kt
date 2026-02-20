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

    // NEW: BroadcastReceiver to listen for hardware changes "like before"
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Check permissions and hardware state whenever BT or Location status changes
            refreshState()
        }
    }

    init {
        // Register for Bluetooth and Location state changes
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        activity.registerReceiver(stateReceiver, filter)
        refreshState()
    }

    // Call this when the ViewModel/Screen is destroyed
    fun teardown() {
        try {
            activity.unregisterReceiver(stateReceiver)
        } catch (e: Exception) { /* Already unregistered */ }
    }

    /**
     * Checks the current state without necessarily launching prompts.
     * This is what gives the UI its "automatic" feel.
     */
    fun refreshState() {
        val permissions = requiredPermissions()
        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            !hasPermissions -> _state.value = BluetoothPermissionState.PermissionsRequired
            !isBluetoothEnabled() -> _state.value = BluetoothPermissionState.BluetoothDisabled
            !isLocationEnabled() -> _state.value = BluetoothPermissionState.LocationDisabled
            else -> _state.value = BluetoothPermissionState.Ready
        }
    }

    fun ensureReady(onReady: () -> Unit) {
        refreshState()

        when (val currentState = _state.value) {
            is BluetoothPermissionState.PermissionsRequired -> {
                permissionLauncher.launch(requiredPermissions())
            }
            is BluetoothPermissionState.BluetoothDisabled -> {
                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            is BluetoothPermissionState.LocationDisabled -> {
                locationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            is BluetoothPermissionState.Ready -> {
                onReady()
            }
            else -> { /* Handle denied states */ }
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
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        } else {
            // Android 6.0 to 11
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}