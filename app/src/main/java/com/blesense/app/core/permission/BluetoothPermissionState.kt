package com.blesense.app.core.permission

sealed class BluetoothPermissionState {
    object Idle : BluetoothPermissionState()
    object PermissionsRequired : BluetoothPermissionState()
    object PermissionDenied : BluetoothPermissionState()
    object BluetoothDisabled : BluetoothPermissionState()
    object LocationDisabled : BluetoothPermissionState()
    object Ready : BluetoothPermissionState()
}
