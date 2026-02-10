package com.blesense.app.features.bluetooth.data.datasourse

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

class BlePermissionChecker {

    fun hasScanPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}
