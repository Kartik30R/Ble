package com.blesense.app.Presentation

import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

import com.blesense.app.core.permission.BluetoothPermissionManager
import com.blesense.app.core.permission.BluetoothPermissionState
import com.blesense.app.coreui.theme.ThemeManager
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
    val sensorTypes = listOf("SHT40", "LIS2DH", "Lux Sensor", "Soil Sensor", "DataLogger", "TempLogger")
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
        permissionManager.refreshState()
    }

    DisposableEffect(Unit) {
        onDispose {
            permissionManager.teardown()
        }
    }

    /* ---------------- Filtering Logic ---------------- */

    val filteredDevices =
        if (selectedSensors.isEmpty()) {
            bluetoothDevices
        } else {
            bluetoothDevices.filter { device ->
                selectedSensors.any { sensor ->
                    device.name?.contains(sensor, ignoreCase = true) == true
                }
            }
        }

    val devicesToShow =
        if (showAllDevices) filteredDevices
        else filteredDevices.take(4)

    /* ---------------- UI ---------------- */

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = "Nearby Devices",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        ThemeManager.toggleDarkMode(!isDarkMode)
                    }) {
                        Icon(
                            imageVector = if (isDarkMode)
                                Icons.Default.LightMode
                            else Icons.Default.DarkMode,
                            contentDescription = "Theme"
                        )
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
                    .padding(top = 8.dp)
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
                            style = MaterialTheme.typography.titleMedium
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

                            FilterChip(
                                selected = selectedSensors.contains(sensor),
                                onClick = {
                                    selectedSensors =
                                        if (selectedSensors.contains(sensor))
                                            selectedSensors - sensor
                                        else
                                            selectedSensors + sensor
                                },
                                label = { Text(sensor) }
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

/* ---------------- Permission Banner ---------------- */

@Composable
fun PermissionStatusBanner(
    state: BluetoothPermissionState,
    manager: BluetoothPermissionManager,
    viewModel: BluetoothScanViewModel
) {
    if (state == BluetoothPermissionState.Ready) return

    val message = when (state) {
        BluetoothPermissionState.PermissionDenied ->
            "Bluetooth permissions are required to scan."
        BluetoothPermissionState.BluetoothDisabled ->
            "Bluetooth is turned off."
        BluetoothPermissionState.LocationDisabled ->
            "Location services are required for BLE discovery."
        else -> null
    }

    message?.let {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (isExpanded) "Show Less" else "Show More",
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge
        )
    }
}