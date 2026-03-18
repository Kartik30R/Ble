//package com.blesense.app.features.bluetooth.presentation.screens.dataLogger
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.os.Build
//import android.util.Log
//
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.lazy.rememberLazyListState
//
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Refresh
//
//import androidx.compose.material3.*
//
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//
//import androidx.navigation.NavController
//
//import com.blesense.app.core.permission.BluetoothPermissionManager
//
//import com.blesense.app.coreui.constants.AppStrings
//import com.blesense.app.features.bluetooth.data.datasourse.BleCommandSender
//import com.blesense.app.features.bluetooth.domain.model.BleDevice
//import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DataLoggerPacketCard
//import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DraggableScrollbar
//
//import presentation.viewmodel.BluetoothScanViewModel
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@SuppressLint("MissingPermission")
//@Composable
//fun DataLoggerScreen(
//    deviceAddress: String,
//    deviceName: String,
//    navController: NavController,
//    deviceId: String,
//    viewModel: BluetoothScanViewModel
//) {
//
//    val TAG = "BLE_ADV"
//
//    val context = LocalContext.current
//    val activity = context as? Activity ?: return
//
//    val coroutineScope = rememberCoroutineScope()
//
//    /* ---------------- Permission manager ---------------- */
//
//    lateinit var permissionManager: BluetoothPermissionManager
//
//    val permissionLauncher =
//        rememberLauncherForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { result ->
//
//            val granted = result.values.all { it }
//
//            Log.d(TAG, "Permission result = $granted")
//
//            permissionManager.onPermissionResult(granted) {
//
//                viewModel.startScan()
//            }
//        }
//
//    val bluetoothLauncher =
//        rememberLauncherForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) {
//            permissionManager.onBluetoothResult {
//                viewModel.startScan()
//            }
//        }
//
//    val locationLauncher =
//        rememberLauncherForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) {
//            permissionManager.onLocationResult {
//                viewModel.startScan()
//            }
//        }
//
//    permissionManager =
//        remember {
//
//            BluetoothPermissionManager(
//                activity,
//                permissionLauncher,
//                bluetoothLauncher,
//                locationLauncher
//            )
//        }
//
//    val permissionState by permissionManager.state.collectAsState()
//
//    /* ---------------- Start scan ---------------- */
//
//    LaunchedEffect(Unit) {
//
//        permissionManager.ensureReady {
//
//            Log.d(TAG, "Permissions ready → startScan")
//
//            viewModel.startScan()
//        }
//    }
//
//
//    /* ---------------- State ---------------- */
//
//    var connectedDevice by remember {
//        mutableStateOf<BleDevice?>(null)
//    }
//
//    var isRefreshing by remember {
//        mutableStateOf(false)
//    }
//
//    var isGettingData by remember {
//        mutableStateOf(false)
//    }
//
//    var isResetting by remember {
//        mutableStateOf(false)
//    }
//
//    var lastPacketCount by remember {
//        mutableIntStateOf(0)
//    }
//
//    val packetHistory by viewModel
//        .dataLoggerPacketHistory
//        .collectAsState()
//
//    val devices by viewModel
//        .devices
//        .collectAsState()
//
//    val isScanning by viewModel
//        .isScanning
//        .collectAsState()
//
//    /* ---------------- Device detection ---------------- */
//
//    val currentDevice by remember(devices, deviceAddress) {
//
//        derivedStateOf {
//
//            devices.find {
//
//                it.address == deviceAddress &&
//                        it.name.contains("DataLogger", true)
//
//            } ?: devices.find {
//
//                it.name.contains("DataLogger", true)
//
//            }
//        }
//    }
//
//    LaunchedEffect(devices) {
//
//        val d =
//            devices.find {
//
//                it.name.contains("DataLogger", true)
//            }
//
//        if (d != null) {
//
//            Log.d(TAG, "DataLogger found: ${d.address}")
//
//            connectedDevice = d
//        }
//    }
//
//    DisposableEffect(Unit) {
//
//        onDispose {
//
//            Log.d(TAG, "Stopping scan")
//
//            viewModel.stopScan()
//viewModel.stopAdvertising()
//
//        }
//    }
//
//    /* stop advertising when data arrives */
//
//    LaunchedEffect(packetHistory.size) {
//
//        if (packetHistory.size > lastPacketCount) {
//
//            Log.d(TAG, "Packets received → stop advertising")
//            viewModel.stopAdvertising()
//
//
//            isGettingData = false
//            isResetting = false
//        }
//    }
//
//    /* timeout safety */
//
//    LaunchedEffect(isGettingData, isResetting) {
//
//        if (isGettingData || isResetting) {
//
//            delay(40000)
//
//            Log.d(TAG, "Timeout → stop advertising")
//            viewModel.stopAdvertising()
//
//
//            isGettingData = false
//            isResetting = false
//        }
//    }
//
//    /* ---------------- UI ---------------- */
//
//    Scaffold(
//
//        topBar = {
//
//            TopAppBar(
//
//                title = {
//                    Text(AppStrings.ADVERTISING_DATA_TITLE)
//                },
//
//                navigationIcon = {
//
//                    IconButton({
//
//                        navController.popBackStack()
//
//                    }) {
//
//                        Icon(
//                            Icons.AutoMirrored.Filled.ArrowBack,
//                            null
//                        )
//                    }
//                },
//
//                actions = {
//
//                    IconButton({
//
//                        coroutineScope.launch {
//
//                            isRefreshing = true
//
//                            viewModel.stopScan()
//
//                            delay(500)
//
//                            viewModel.startScan()
//
//                            delay(1500)
//
//                            isRefreshing = false
//                        }
//
//                    }) {
//
//                        if (isRefreshing)
//
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(20.dp)
//                            )
//
//                        else
//
//                            Icon(Icons.Default.Refresh, null)
//                    }
//                }
//            )
//        }
//
//    ) { padding ->
//
//        Column(
//
//            Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//
//        ) {
//
//            /* device info */
//
//            Card(Modifier.fillMaxWidth()) {
//
//                Column(Modifier.padding(16.dp)) {
//
//                    Text(
//                        currentDevice?.name
//                            ?: "Searching DataLogger..."
//                    )
//
//                    Text(
//                        currentDevice?.address
//                            ?: deviceAddress
//                    )
//
//                    Text(
//                        if (currentDevice != null)
//                            "Receiving packets"
//                        else if (isScanning)
//                            "Scanning..."
//                        else
//                            "Idle"
//                    )
//
//                    Text(
//                        "Packets received: ${packetHistory.size}"
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            /* buttons */
//
//            Row {
//
//                Button(
//
//                    modifier = Modifier.weight(1f),
//
//                    onClick = {
//
//                        Log.d(TAG, "DOWNLOAD CLICKED")
//
//                        if (!isGettingData) {
//
//                            isGettingData = true
//
//                            lastPacketCount =
//                                packetHistory.size
//                            viewModel.requestDataLoggerDownload()
//
//
//                        }
//                    }
//
//                ) {
//
//                    if (isGettingData)
//
//                        CircularProgressIndicator(
//                            modifier =
//                                Modifier.size(18.dp)
//                        )
//
//                    else
//
//                        Text("Download")
//                }
//
//                Spacer(Modifier.width(12.dp))
//
//                OutlinedButton(
//                    modifier = Modifier.weight(1f),
//                    onClick = {
//
//                        Log.d(TAG, "RESET CLICKED")
//
//                        if (!isResetting) {
//
//                            isResetting = true
//
//                            viewModel.requestReset()
//                        }
//                    }
//                ) {
//
//                    if (isResetting)
//
//                        CircularProgressIndicator(
//                            modifier =
//                                Modifier.size(18.dp)
//                        )
//
//                    else
//
//                        Text("Reset")
//                }
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            /* packet list */
//
//            val listState =
//                rememberLazyListState()
//
//            Box(Modifier.fillMaxSize()) {
//
//                if (packetHistory.isEmpty()) {
//
//                    Box(
//                        Modifier.fillMaxSize(),
//                        contentAlignment =
//                            Alignment.Center
//                    ) {
//
//                        CircularProgressIndicator()
//                    }
//                }
//
//                else {
//
//                    LazyColumn(
//                        state = listState
//                    ) {
//
//                        itemsIndexed(
//                            packetHistory
//                                .sortedByDescending {
//                                    it.currentPacketId
//                                }
//                        ) { index, packet ->
//
//                            DataLoggerPacketCard(
//                                packet,
//                                index == 0
//                            )
//                        }
//                    }
//                }
//
//                DraggableScrollbar(
//                    listState,
//                    Modifier.align(
//                        Alignment.CenterEnd
//                    )
//                )
//            }
//        }
//    }
//}



package com.blesense.app.features.bluetooth.presentation.screens.dataLogger

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blesense.app.core.permission.BluetoothPermissionManager
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DataLoggerPacketCard
import com.blesense.app.features.bluetooth.presentation.widget.dataLogger.DraggableScrollbar
import presentation.viewmodel.BluetoothScanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
 import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataLoggerScreen(
    deviceAddress: String,
    deviceName: String,
    navController: NavController,
    deviceId: String,
    viewModel: BluetoothScanViewModel
) {

    val TAG = "BLE_UI"

    val context = LocalContext.current
    val activity = context as? Activity ?: return

    val coroutineScope = rememberCoroutineScope()

    /* ---------------- PERMISSIONS ---------------- */

    lateinit var permissionManager: BluetoothPermissionManager

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            permissionManager.onPermissionResult(
                granted.values.all { it }
            ) { viewModel.startScan() }
        }

    val bluetoothLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            permissionManager.onBluetoothResult {
                viewModel.startScan()
            }
        }

    val locationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            permissionManager.onLocationResult {
                viewModel.startScan()
            }
        }

    permissionManager =
        remember {
            BluetoothPermissionManager(
                activity,
                permissionLauncher,
                bluetoothLauncher,
                locationLauncher
            )
        }

    LaunchedEffect(Unit) {
        permissionManager.ensureReady {
            viewModel.startScan()
        }
    }

    /* ---------------- STATE ---------------- */

    val devices by viewModel.devices.collectAsState()
    val packetHistory by viewModel.dataLoggerPacketHistory.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var connectedDevice by remember {
        mutableStateOf<BleDevice?>(null)
    }
    var isExporting by remember {
        mutableStateOf(false)
    }

    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri: Uri? ->

            if (uri == null || packetHistory.isEmpty())
                return@rememberLauncherForActivityResult

            coroutineScope.launch {

                isExporting = true

                withContext(Dispatchers.IO) {

                    try {

                        context.contentResolver
                            .openOutputStream(uri)
                            ?.use { stream ->

                                val df =
                                    SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss.SSS",
                                        Locale.getDefault()
                                    )

                                val header =
                                    "Timestamp,Packet_ID,Last_Packet_ID,Device_ID,Raw_Data_Bytes,Raw_Hex_String\n"

                                stream.write(
                                    header.toByteArray()
                                )

                                packetHistory
                                    .sortedByDescending {
                                        it.currentPacketId
                                    }
                                    .forEach { packet ->

                                        val bytes =
                                            packet.rawData
                                                .split(" ")
                                                .size

                                        val line =
                                            "${df.format(Date(packet.timestamp))}," +
                                                    "${packet.currentPacketId}," +
                                                    "${packet.lastPacketId}," +
                                                    "${packet.deviceId}," +
                                                    "$bytes," +
                                                    "\"${packet.rawData}\"\n"

                                        stream.write(
                                            line.toByteArray()
                                        )
                                    }
                            }

                    } catch (e: Exception) {

                        Log.e("CSV", "Export failed", e)
                    }
                }

                isExporting = false
            }
        }
    var isRefreshing by remember { mutableStateOf(false) }
    var isGettingData by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    var lastPacketCount by remember {
        mutableIntStateOf(0)
    }

    /* ---------------- FIND DEVICE ---------------- */

    val currentDevice by remember(devices, deviceAddress) {

        derivedStateOf {

            devices.find {

                it.address == deviceAddress &&
                        it.name.contains("DataLogger", true)

            } ?: devices.find {

                it.name.contains("DataLogger", true)
            }
        }
    }

    LaunchedEffect(devices) {

        currentDevice?.let {

            connectedDevice = it
        }
    }

    /* stop scan when leaving */

    DisposableEffect(Unit) {

        onDispose {

            viewModel.stopScan()
            viewModel.stopAdvertising()
        }
    }

    /* stop advertising when packets arrive */

    LaunchedEffect(packetHistory.size) {

        if (packetHistory.size > lastPacketCount) {

            viewModel.stopAdvertising()

            isGettingData = false
            isResetting = false
        }
    }

    /* timeout safety */

    LaunchedEffect(isGettingData, isResetting) {

        if (isGettingData || isResetting) {

            delay(40000)

            viewModel.stopAdvertising()

            isGettingData = false
            isResetting = false
        }
    }

    /* ---------------- LOST PACKET CALCULATION ---------------- */

    val lostPacketIds =
        remember(packetHistory) {

            val ids =
                packetHistory.map {
                    it.currentPacketId
                }

            if (ids.size < 2)
                emptyList()

            else {

                val min = ids.minOrNull()!!
                val max = ids.maxOrNull()!!

                val set = ids.toSet()

                (min..max)
                    .filter { it !in set }
            }
        }

    /* ---------------- THEME BACKGROUND ---------------- */

    val backgroundGradient =
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.background
            )
        )

    val textColor =
        MaterialTheme.colorScheme.onPrimary

    /* ---------------- UI ---------------- */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {

        Scaffold(

            containerColor = Color.Transparent,

            topBar = {

                TopAppBar(

                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),

                    title = {

                        Text(
                            AppStrings.ADVERTISING_DATA_TITLE,
                            color = textColor
                        )
                    },

                    navigationIcon = {

                        IconButton({

                            navController.popBackStack()

                        }) {

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = textColor
                            )
                        }
                    },

                    actions = {

                        IconButton({

                            coroutineScope.launch {

                                isRefreshing = true

                                viewModel.stopScan()

                                delay(1000)

                                viewModel.startScan()

                                delay(3000)

                                isRefreshing = false
                            }

                        }) {

                            if (isRefreshing)

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(20.dp),
                                    color = textColor
                                )

                            else

                                Icon(
                                    Icons.Default.Refresh,
                                    null,
                                    tint = textColor
                                )
                        }
                    }
                )
            }

        ) { padding ->

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)

            ) {

                /* DEVICE INFO */

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        )
                ) {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            "Device: ${
                                currentDevice?.name
                                    ?: "Data Logger"
                            }"
                        )

                        Text(
                            currentDevice?.address
                                ?: deviceAddress
                        )

                        Spacer(
                            Modifier.height(6.dp)
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .background(
                                            if (connectedDevice != null)
                                                Color.Green
                                            else
                                                Color.Red,
                                            CircleShape
                                        )
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text(

                                if (connectedDevice != null)
                                    "Connected"
                                else if (isScanning)
                                    "Scanning..."
                                else
                                    "Idle"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* ACTION BUTTONS */

                Row {

                    Button(

                        modifier =
                            Modifier.weight(1f),

                        onClick = {

                            if (!isGettingData) {

                                isGettingData = true

                                lastPacketCount =
                                    packetHistory.size

                                viewModel
                                    .requestDataLoggerDownload()
                            }
                        }

                    ) {

                        if (isGettingData)

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp)
                            )

                        else

                            Text("Get Data")
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(

                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            ),

                        onClick = {

                            if (!isResetting) {

                                isResetting = true

                                viewModel.requestReset()
                            }
                        }

                    ) {

                        if (isResetting)

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp)
                            )

                        else

                            Text("Reset Device")
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* STATISTICS */

                Card {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text("Total Packets Arrival")

                        Text(
                            packetHistory.size.toString(),
                            style =
                                MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Card {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text("Packets Lost")

                        Text(
                            lostPacketIds.size.toString(),
                            style =
                                MaterialTheme.typography.headlineMedium,
                            color =
                                if (lostPacketIds.isEmpty())
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                Button(

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        packetHistory.isNotEmpty() &&
                                !isExporting,

                    onClick = {

                        val timestamp =
                            SimpleDateFormat(
                                "yyyyMMdd_HHmmss",
                                Locale.getDefault()
                            ).format(Date())

                        val fileName =
                            "DataLogger_Raw_${deviceId}_$timestamp.csv"

                        createDocumentLauncher.launch(fileName)
                    }

                ) {

                    if (isExporting) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Text("Exporting CSV...")
                        }
                    }

                    else {

                        Text("Export All Raw Data as CSV")
                    }
                }
                Spacer(Modifier.height(16.dp))

                /* PACKET LIST */

                val listState =
                    rememberLazyListState()

                Box(
                    Modifier.fillMaxSize()
                ) {

                    if (packetHistory.isEmpty()) {

                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator()
                        }
                    }

                    else {

                        LazyColumn(
                            state = listState
                        ) {

                            itemsIndexed(
                                packetHistory
                                    .sortedByDescending {
                                        it.currentPacketId
                                    }
                            ) { index, packet ->

                                DataLoggerPacketCard(
                                    packet,
                                    index == 0
                                )
                            }
                        }
                    }

                    DraggableScrollbar(
                        listState,
                        Modifier.align(
                            Alignment.CenterEnd
                        )
                    )
                }
            }
        }
    }
}