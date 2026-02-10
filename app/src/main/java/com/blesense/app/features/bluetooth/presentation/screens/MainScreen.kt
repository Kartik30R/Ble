package com.blesense.app.Presentation

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.* // Using Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
 import com.blesense.app.ThemeManager
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.presentation.widget.BluetoothDeviceItem
import presentation.viewmodel.BluetoothScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    bluetoothViewModel: BluetoothScanViewModel
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // 1. Collect state from New ViewModel
    val bluetoothDevices by bluetoothViewModel.devices.collectAsState()
    val isScanning by bluetoothViewModel.isScanning.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    // 2. State for UI Logic
    var expanded by remember { mutableStateOf(false) }
    var showAllDevices by remember { mutableStateOf(false) }
    val sensorTypes = listOf("SHT40", "LIS2DH", "Lux Sensor", "Soil Sensor", "DataLogger", "TempLogger")
    var selectedSensor by remember { mutableStateOf(sensorTypes[0]) }

    // Bluetooth setup
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    // 3. Lifecycle: Automatic Scanning
    DisposableEffect(Unit) {
        bluetoothViewModel.startScan(activity)
        onDispose { bluetoothViewModel.stopScan() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background, // Central Theme Background
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = AppStrings.APP_NAME,
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
                    IconButton(onClick = { ThemeManager.toggleDarkMode(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.primary
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    // Main Container Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby Devices (${bluetoothDevices.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row {
                                    IconButton(onClick = { bluetoothViewModel.startScan(activity) }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                    }

                                    Box {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Filter")
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            sensorTypes.forEach { sensor ->
                                                DropdownMenuItem(
                                                    text = { Text(sensor) },
                                                    onClick = {
                                                        selectedSensor = sensor
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4. Reactive UI Content
                            if (bluetoothDevices.isEmpty()) {
                                EmptyStateView(isScanning)
                            } else {
                                val devicesToShow = if (showAllDevices) bluetoothDevices else bluetoothDevices.take(4)

                                devicesToShow.forEach { device ->
                                    BluetoothDeviceItem(
                                        device = device,
                                        navController = navController,
                                        selectedSensor = selectedSensor,
                                        isDarkMode = isDarkMode
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }

                                if (bluetoothDevices.size > 4) {
                                    ShowMoreToggle(showAllDevices) { showAllDevices = !showAllDevices }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isScanning: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isScanning) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Searching for sensors...", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("No devices in range", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}