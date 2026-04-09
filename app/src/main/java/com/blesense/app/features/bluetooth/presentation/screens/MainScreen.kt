package com.blesense.app.Presentation

import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

import com.blesense.app.core.permission.BluetoothPermissionManager
import com.blesense.app.core.permission.BluetoothPermissionState
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.*
import com.blesense.app.features.bluetooth.presentation.widget.BluetoothDeviceItem
import presentation.viewmodel.BluetoothScanViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothScanViewModel
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    val bluetoothDevices by bluetoothViewModel.devices.collectAsState()
    val isScanning by bluetoothViewModel.isScanning.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    var showAllDevices by remember { mutableStateOf(false) }
    val sensorTypes = listOf(
        "SHT40", "LIS3DH", "Lux Sensor", "Soil Sensor",
        "Speed Distance", "Ammonia Sensor", "DataLogger",
        "TempLogger", "SEN6x"
    )
    var selectedSensors by remember { mutableStateOf(setOf<String>()) }
    val coroutineScope = rememberCoroutineScope()

    /* ---------------- Permission Setup ---------------- */
    lateinit var permissionManager: BluetoothPermissionManager

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionManager.onPermissionResult(granted) {
            bluetoothViewModel.startScan()
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionManager.onBluetoothResult {
            bluetoothViewModel.startScan()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionManager.onLocationResult {
            bluetoothViewModel.startScan()
        }
    }

    permissionManager = remember(activity) {
        BluetoothPermissionManager(
            activity = activity,
            permissionLauncher = permissionLauncher,
            bluetoothLauncher = bluetoothLauncher,
            locationLauncher = locationLauncher
        )
    }

    val permissionState by permissionManager.state.collectAsState()

    LaunchedEffect(permissionState) {
        if (permissionState == BluetoothPermissionState.Ready) {
            bluetoothViewModel.startScan()
        }
    }

    LaunchedEffect(Unit) {
        permissionManager.ensureReady {
            bluetoothViewModel.startScan()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            permissionManager.teardown()
        }
    }

    /* ---------------- Filtering Logic ---------------- */
    val filteredDevices = if (selectedSensors.isEmpty()) {
        bluetoothDevices
    } else {
        bluetoothDevices.filter { device ->
            selectedSensors.any { sensor ->
                matchesSensorType(device.name, sensor)
            }
        }
    }

    val devicesToShow = if (showAllDevices) filteredDevices else filteredDevices.take(4)

    /* ---------------- UI ---------------- */
    Box(modifier = Modifier.fillMaxSize().neumorphicBackground()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Text(
                            text = "Nearby Devices",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                PermissionStatusBanner(permissionState, permissionManager, bluetoothViewModel)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    /* -------- Total + Refresh -------- */
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Devices: ${bluetoothDevices.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )

                            IconButton(
                                onClick = {
                                    permissionManager.ensureReady {
                                        coroutineScope.launch {
                                            bluetoothViewModel.stopScan()
                                            bluetoothViewModel.clearDevices()
                                            delay(500)
                                            bluetoothViewModel.startScan()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MintGreenAccent)
                            }
                        }
                    }

                    /* -------- Filter Chips -------- */
                    item {
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sensorTypes.forEach { sensor ->
                                NeonPillButton(
                                    text = sensor,
                                    isActive = selectedSensors.contains(sensor),
                                    onClick = {
                                        selectedSensors = if (selectedSensors.contains(sensor))
                                            selectedSensors - sensor
                                        else
                                            selectedSensors + sensor
                                    }
                                )
                            }
                        }
                    }

                    /* -------- Device List -------- */
                    if (devicesToShow.isEmpty()) {
                        item {
                            EmptyStateView(isScanning)
                        }
                    } else {
                        items(devicesToShow.size) { index ->
                            BluetoothDeviceItem(
                                device = devicesToShow[index],
                                navController = navController,
                                selectedSensor = selectedSensors.firstOrNull() ?: sensorTypes.first(),
                                isDarkMode = isDarkMode
                            )
                        }

                        if (filteredDevices.size > 4) {
                            item {
                                ShowMoreToggle(showAllDevices) {
                                    showAllDevices = !showAllDevices
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Permission Banner ---------------- */
@Composable
fun PermissionStatusBanner(
    state: BluetoothPermissionState,
    manager: BluetoothPermissionManager,
    viewModel: BluetoothScanViewModel
) {
    if (state == BluetoothPermissionState.Ready) return

    val message = when (state) {
        BluetoothPermissionState.PermissionDenied -> "Bluetooth permissions are required to scan."
        BluetoothPermissionState.BluetoothDisabled -> "Bluetooth is turned off."
        BluetoothPermissionState.LocationDisabled -> "Location services are required for BLE discovery."
        else -> null
    }

    message?.let {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
                TextButton(onClick = {
                    manager.ensureReady { viewModel.startScan() }
                }) {
                    Text("FIX")
                }
            }
        }
    }
}

/* ---------------- Empty State ---------------- */
@Composable
fun EmptyStateView(isScanning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isScanning) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Searching for sensors...")
        } else {
            Text("No devices in range")
        }
    }
}

/* ---------------- Show More Toggle ---------------- */
@Composable
fun ShowMoreToggle(isExpanded: Boolean, onClick: () -> Unit) {
    NeonPillButton(
        text = if (isExpanded) "Show Less" else "Show More",
        onClick = onClick,
        isActive = false, // renders inactive dark outline button
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)
    )
}

private fun matchesSensorType(deviceName: String?, selectedType: String): Boolean {
    if (deviceName == null) return false
    return when (selectedType) {
        "SHT40" -> deviceName.contains("SHT", true)
        "LIS3DH" -> deviceName.contains("Activity", true)
        "Lux Sensor" -> deviceName.contains("Lux_Data", true)
        "Soil Sensor" -> deviceName.contains("SOIL", true)
        "Speed Distance" -> deviceName.contains("Speed", true)
        "Ammonia Sensor" -> deviceName.contains("NH", true)
        "DataLogger" -> deviceName.contains("DataLogger", true) ||
                deviceName.contains("Data Logger", true) ||
                deviceName.contains("DLOG", true)
        "TempLogger" -> deviceName.contains("TempLogger", true) ||
                deviceName.contains("Temp Logger", true) ||
                deviceName.contains("TLOG", true)
        "SEN6x" -> deviceName.contains("SEN", true)
        else -> false
    }
}