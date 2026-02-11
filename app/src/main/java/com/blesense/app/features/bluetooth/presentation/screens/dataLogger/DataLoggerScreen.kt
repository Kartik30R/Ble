package com.blesense.app.features.bluetooth.presentation.screens.dataLogger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.blesense.app.core.permission.BluetoothPermissionManager
import com.blesense.app.core.permission.BluetoothPermissionState

import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.data.datasourse.BleCommandSender
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DataLoggerPacketCard
import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DraggableScrollbar
import presentation.viewmodel.BluetoothScanViewModel

 import kotlinx.coroutines.delay



@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun DataLoggerScreen(
    deviceAddress: String,
    deviceName: String,
    navController: NavController,
    deviceId: String,
    viewModel: BluetoothScanViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val coroutineScope = rememberCoroutineScope()

    /* ------------------------------------------------ */
    /* Permission Manager Setup */
    /* ------------------------------------------------ */

    lateinit var permissionManager: BluetoothPermissionManager

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionManager.onPermissionResult(granted) {
            viewModel.startScan(activity)
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionManager.onBluetoothResult {
            viewModel.startScan(activity)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionManager.onLocationResult {
            viewModel.startScan(activity)
        }
    }

    permissionManager = remember {
        BluetoothPermissionManager(
            activity = activity,
            permissionLauncher = permissionLauncher,
            bluetoothLauncher = bluetoothLauncher,
            locationLauncher = locationLauncher
        )
    }

    val permissionState by permissionManager.state.collectAsState()

    /* ------------------------------------------------ */
    /* Start Flow */
    /* ------------------------------------------------ */

    LaunchedEffect(Unit) {
        permissionManager.ensureReady {
            viewModel.startScan(activity)
        }
    }

    /* ------------------------------------------------ */
    /* Existing Logic (UNCHANGED) */
    /* ------------------------------------------------ */

    val commandSender = remember { BleCommandSender(context) }

    var connectedDevice by remember { mutableStateOf<BleDevice?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isGettingData by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var lastPacketCount by remember { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    val packetHistory by viewModel.dataLoggerPacketHistory.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val lostPacketIds = remember(packetHistory) {
        val ids = packetHistory.map { it.currentPacketId }
        if (ids.size < 2) emptyList()
        else {
            val present = ids.toSet()
            val max = ids.maxOrNull() ?: 0
            val min = ids.minOrNull() ?: 0
            (min..max).filter { it !in present }
        }
    }

    val currentDevice by remember(devices, deviceAddress) {
        derivedStateOf {
            devices.find { it.address == deviceAddress }
                ?: devices.find {
                    it.name.contains("DataLogger", true) ||
                            it.name.contains("Data Logger", true)
                }
        }
    }

    LaunchedEffect(devices) {
        val d = devices.find {
            it.name.contains("DataLogger", true) ||
                    it.name.contains("Data Logger", true)
        }
        d?.let { connectedDevice = it }
    }

    DisposableEffect(navController) {
        onDispose { viewModel.stopScan() }
    }

    LaunchedEffect(packetHistory.size) {
        if (packetHistory.size > lastPacketCount) {
            commandSender.stopAdvertising()
            isGettingData = false
            isResetting = false
        }
    }

    LaunchedEffect(isGettingData, isResetting) {
        if (isGettingData || isResetting) {
            delay(40000)
            isGettingData = false
            isResetting = false
        }
    }

    /* ------------------------------------------------ */
    /* UI */
    /* ------------------------------------------------ */

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        AppStrings.ADVERTISING_DATA_TITLE,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopScan()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            /* Permission State Feedback */

            when (permissionState) {
                BluetoothPermissionState.PermissionDenied -> {
                    Text(
                        text = "Permission Denied",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }
                BluetoothPermissionState.BluetoothDisabled -> {
                    Text(
                        text = "Bluetooth Disabled",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }
                BluetoothPermissionState.LocationDisabled -> {
                    Text(
                        text = "Location Disabled",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }
                else -> {}
            }

            Text(
                text = "${AppStrings.DEVICE_NAME_LABEL}: ${currentDevice?.address ?: deviceAddress}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "${AppStrings.NODE_ID_LABEL}: $deviceId",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                Button(
                    onClick = {
                        if (!isGettingData) {
                            isGettingData = true
                            lastPacketCount = packetHistory.size
                            commandSender.sendCommand(
                                byteArrayOf(0xBB.toByte(), 0xCC.toByte()),
                                40000
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(AppStrings.DOWNLOAD_DATA)
                }

                OutlinedButton(
                    onClick = {
                        if (!isResetting) {
                            isResetting = true
                            commandSender.sendCommand(
                                byteArrayOf(0xFF.toByte(), 0xFF.toByte()),
                                40000
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(AppStrings.RESET_STEPS)
                }
            }

            Spacer(Modifier.height(24.dp))

            val listState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(packetHistory) { index, packet ->
                        DataLoggerPacketCard(
                            packet = packet,
                            isFirst = index == 0
                        )
                    }
                }

                DraggableScrollbar(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}
