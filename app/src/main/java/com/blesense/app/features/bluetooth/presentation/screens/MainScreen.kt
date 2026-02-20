    package com.blesense.app.Presentation

    import android.bluetooth.BluetoothAdapter
    import android.util.Log
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
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
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        navController: NavHostController,
        bluetoothViewModel: BluetoothScanViewModel
    ) {
        val context = LocalContext.current
        val activity = context as ComponentActivity

        // UI State
        val bluetoothDevices by bluetoothViewModel.devices.collectAsState()
        val isScanning by bluetoothViewModel.isScanning.collectAsState()
        val isDarkMode by ThemeManager.isDarkMode.collectAsState()

        var expanded by remember { mutableStateOf(false) }
        var showAllDevices by remember { mutableStateOf(false) }
        val sensorTypes = listOf("SHT40", "LIS2DH", "Lux Sensor", "Soil Sensor", "DataLogger", "TempLogger")
        var selectedSensor by remember { mutableStateOf(sensorTypes[0]) }

        /* ------------------------------------------------ */
        /* Permission Manager Setup (Launchers First)       */
        /* ------------------------------------------------ */

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

        /* ------------------------------------------------ */
        /* Initial Permission State Check                   */
        /* ------------------------------------------------ */

        LaunchedEffect(Unit) {
            permissionManager.refreshState()
        }

        /* ------------------------------------------------ */
        /* Scan Reactivity                                  */
        /* ------------------------------------------------ */



        /* ------------------------------------------------ */
        /* Cleanup                                          */
        /* ------------------------------------------------ */

        DisposableEffect(Unit) {
            onDispose {
                permissionManager.teardown()
            }
        }
        /* ------------------------------------------------ */
        /* UI Layout                                        */
        /* ------------------------------------------------ */

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = "BLE Sense",
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

                 PermissionStatusBanner(permissionState, permissionManager, bluetoothViewModel)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

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
                                        IconButton(onClick = {
                                            permissionManager.ensureReady {
                                                bluetoothViewModel.stopScan()
                                                bluetoothViewModel.clearDevices()
                                                bluetoothViewModel.startScan( )
                                            }
                                        }) {
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

                                if (bluetoothDevices.isEmpty()) {
                                    EmptyStateView(isScanning)
                                } else {
                                    val devicesToShow = if (showAllDevices) bluetoothDevices else bluetoothDevices.take(4)

                                    devicesToShow.forEach { device ->
                                         val displayName = when {
                                            !device.name.isNullOrBlank() -> device.name
                                            device.sensorData != null -> selectedSensor
                                            else -> "Scanning..."
                                        }

                                        BluetoothDeviceItem(
                                             device = device.copy(name = displayName),
                                            navController = navController,
                                            selectedSensor = selectedSensor,
                                            isDarkMode = isDarkMode
                                        )
                                    }

                                    if (bluetoothDevices.size > 4) {
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
    }

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
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = {
                        manager.ensureReady { viewModel.startScan( ) }
                    }) {
                        Text("FIX")
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