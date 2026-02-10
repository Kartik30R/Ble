//package com.blesense.app.Presentation
//
//import android.app.Activity
//import android.content.Context
//import android.media.MediaPlayer
//import android.net.Uri
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.animation.core.RepeatMode
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.infiniteRepeatable
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.itemsIndexed
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Color.Companion.Red
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.text.font.FontStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.blesense.app.DataLoggerDisplay
//import com.blesense.app.R
//import com.blesense.app.ThemeManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.MainScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
///**
// * Data class to hold text for the Advertising Data screen.
// * This provides localization support for UI strings in the advertising data display.
// */
//data class AdvertisingText(
//    val advertisingDataTitle: String = "Advertising Data",
//    val deviceNameLabel: String = "Device Name",
//    val nodeIdLabel: String = "Node ID",
//    val downloadData: String = "DOWNLOAD DATA",
//    val exportingData: String = "EXPORTING DATA...",
//    val temperature: String = "Temperature",
//    val humidity: String = "Humidity",
//    val xAxis: String = "X-Axis",
//    val yAxis: String = "Y-Axis",
//    val zAxis: String = "Z-Axis",
//    val nitrogen: String = "Nitrogen",
//    val phosphorus: String = "Phosphorus",
//    val potassium: String = "Potassium",
//    val moisture: String = "Moisture",
//    val electricConductivity: String = "Electric Conductivity",
//    val pH: String = "pH",
//    val salinity: String = "Salinity",
//    val lightIntensity: String = "Light Intensity",
//    val speed: String = "Speed",
//    val distance: String = "Distance",
//    val objectDetected: String = "Object Detected",
//    val steps: String = "Steps",
//    val ammonia: String = "Ammonia",
//    val resetSteps: String = "RESET STEPS",
//    val warningTitle: String = "Warning",
//    val warningMessage: String = "The %s has exceeded the threshold of %s!",
//    val dismissButton: String = "Dismiss",
//    val rawData: String = "Raw Data"
//)
//
///**
// * Main composable function for the Advertising Data screen.
// * Displays real-time sensor data from a specific BLE device with visualization,
// * threshold alarms, data export, and various sensor type support.
// *
// * @param deviceAddress MAC address of the BLE device
// * @param deviceName Human-readable name of the device
// * @param navController For navigation between screens
// * @param deviceId Unique identifier for the device/node
// * @param viewModel ViewModel handling BLE scanning and data processing
// */
//@Composable
//fun AdvertisingDataScreen(
//    deviceAddress: String,
//    deviceName: String,
//    navController: NavController,
//    deviceId: String,
//    viewModel: BluetoothScanViewModel<Any?>
//) {
//    val context = LocalContext.current
//    val activity = context as? Activity
//
//    // Initialize ViewModel with factory pattern
//    val viewModel: BluetoothScanViewModel<Any> = viewModel(
//        factory = remember { BluetoothScanViewModelFactory(context) }
//    )
//
//    // Start BLE scanning when activity becomes available
//    LaunchedEffect(activity) {
//        activity?.let { viewModel.startScan(it) }
//    }
//
//    // MediaPlayer for alarm sound with loop capability
//    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
//
//    // Initialize MediaPlayer once
//    LaunchedEffect(Unit) {
//        mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply {
//            isLooping = true
//        }
//    }
//
//    // Clean up MediaPlayer when composable is disposed
//    DisposableEffect(Unit) {
//        onDispose {
//            mediaPlayer?.let { player ->
//                if (player.isPlaying) {
//                    player.stop()
//                }
//                player.reset()
//                player.release()
//            }
//            mediaPlayer = null
//        }
//    }
//
//    // Collect theme and device states
//    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
//    val devices by viewModel.devices.collectAsState()
//
//    // Find the current device from the list of discovered devices
//    val currentDevice by remember(devices, deviceAddress) {
//        derivedStateOf { devices.find { it.address == deviceAddress } }
//    }
//
//    // State for threshold configuration and alarm management
//    var thresholdValue by remember { mutableStateOf("") }
//    var isAlarmActive by remember { mutableStateOf(false) }
//    var showAlertDialog by remember { mutableStateOf(false) }
//    var parameterType by remember { mutableStateOf("Temperature") }
//    var isThresholdSet by remember { mutableStateOf(false) }
//
//    val scope = rememberCoroutineScope()
//
//    // Blinking animation for alarm visualization
//    val isBlinking by remember(isAlarmActive) {
//        derivedStateOf { isAlarmActive }
//    }
//
//    val blinkAlpha by animateFloatAsState(
//        targetValue = if (isBlinking) 0.5f else 0f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 500),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "blinkAlpha"
//    )
//
//    // Extract ammonia value from sensor data with safe parsing
//    val ammoniaValue by remember(currentDevice?.sensorData) {
//        derivedStateOf {
//            when (val sensorData = currentDevice?.sensorData) {
//                is BluetoothScanViewModel.SensorData.AmmoniaSensorData ->
//                    sensorData.ammonia.replace(" ppm", "").toFloatOrNull() ?: 0f
//                else -> 0f
//            }
//        }
//    }
//
//    // Debounced ammonia value to prevent rapid UI updates
//    var displayedAmmoniaValue by remember { mutableStateOf(0f) }
//    val debouncedAmmoniaValue by remember(ammoniaValue) {
//        derivedStateOf { ammoniaValue }
//    }
//
//    LaunchedEffect(debouncedAmmoniaValue) {
//        displayedAmmoniaValue = debouncedAmmoniaValue
//    }
//
//    // Extract lux value from sensor data
//    val luxValue by remember(currentDevice?.sensorData) {
//        derivedStateOf {
//            when (val sensorData = currentDevice?.sensorData) {
//                is BluetoothScanViewModel.SensorData.LuxSensorData ->
//                    sensorData.lux.toFloatOrNull() ?: 0f
//                else -> 0f
//            }
//        }
//    }
//
//    // Threshold monitoring and alarm triggering logic
//    LaunchedEffect(currentDevice, thresholdValue, parameterType, isThresholdSet) {
//        delay(500L) // Debounce delay
//        if (isThresholdSet) {
//            val threshold = thresholdValue.toFloatOrNull()
//            if (threshold != null) {
//                // Check different sensor types for threshold violations
//                when (val sensorData = currentDevice?.sensorData) {
//                    is BluetoothScanViewModel.SensorData.SHT40Data -> {
//                        val valueToCheck = when (parameterType) {
//                            "Temperature" -> sensorData.temperature.toFloatOrNull()
//                            "Humidity" -> sensorData.humidity.toFloatOrNull()
//                            else -> null
//                        }
//                        isAlarmActive = valueToCheck != null && valueToCheck > threshold
//                    }
//                    is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> {
//                        isAlarmActive = parameterType == "Ammonia" && ammoniaValue > threshold
//                    }
//                    else -> isAlarmActive = false
//                }
//
//                // Trigger alarm if threshold is exceeded
//                if (isAlarmActive) {
//                    showAlertDialog = true
//                    mediaPlayer?.let { player ->
//                        if (!player.isPlaying) {
//                            try {
//                                player.start()
//                            } catch (e: IllegalStateException) {
//                                // Recreate MediaPlayer if in invalid state
//                                mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply {
//                                    isLooping = true
//                                    start()
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    // Stop alarm if threshold is not exceeded
//                    mediaPlayer?.let { player ->
//                        try {
//                            if (player.isPlaying) {
//                                player.stop()
//                                player.prepare()
//                            }
//                        } catch (e: IllegalStateException) {
//                            // Recreate MediaPlayer if needed
//                            mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply {
//                                isLooping = true
//                            }
//                        }
//                    }
//                    showAlertDialog = false
//                }
//            } else {
//                // Invalid threshold value
//                isAlarmActive = false
//                showAlertDialog = false
//                mediaPlayer?.let { player ->
//                    try {
//                        if (player.isPlaying) {
//                            player.stop()
//                            player.prepare()
//                        }
//                    } catch (e: IllegalStateException) {
//                        mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply {
//                            isLooping = true
//                        }
//                    }
//                }
//            }
//        } else {
//            // Threshold not set
//            isAlarmActive = false
//            showAlertDialog = false
//            mediaPlayer?.let { player ->
//                try {
//                    if (player.isPlaying) {
//                        player.stop()
//                        player.prepare()
//                    }
//                } catch (e: IllegalStateException) {
//                    mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply {
//                        isLooping = true
//                    }
//                }
//            }
//        }
//    }
//
//    // Use fixed English text (localization ready)
//    val advertisingText = AdvertisingText()
//
//    // Transform sensor data into displayable format based on sensor type
//    val displayData by remember(currentDevice?.sensorData, advertisingText) {
//        derivedStateOf {
//            when (val sensorData = currentDevice?.sensorData) {
//                is BluetoothScanViewModel.SensorData.SHT40Data -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
//                    advertisingText.humidity to "${sensorData.humidity.takeIf { it.isNotEmpty() } ?: "0"}%"
//                )
//                is BluetoothScanViewModel.SensorData.SDTData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.speed to "${sensorData.speed.takeIf { it.isNotEmpty() } ?: "0"} m/s",
//                    advertisingText.distance to "${sensorData.distance.takeIf { it.isNotEmpty() } ?: "0"} m"
//                )
//                is BluetoothScanViewModel.SensorData.LIS2DHData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.xAxis to "${sensorData.x.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
//                    advertisingText.yAxis to "${sensorData.y.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
//                    advertisingText.zAxis to "${sensorData.z.takeIf { it.isNotEmpty() } ?: "0"} m/s²"
//                )
//                is BluetoothScanViewModel.SensorData.SoilSensorData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.nitrogen to "${sensorData.nitrogen.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
//                    advertisingText.phosphorus to "${sensorData.phosphorus.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
//                    advertisingText.potassium to "${sensorData.potassium.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
//                    advertisingText.moisture to "${sensorData.moisture.takeIf { it.isNotEmpty() } ?: "0"}%",
//                    advertisingText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
//                    advertisingText.electricConductivity to "${sensorData.ec.takeIf { it.isNotEmpty() } ?: "0"} mS/cm",
//                    advertisingText.pH to "${sensorData.pH.takeIf { it.isNotEmpty() } ?: "0"}",
//                    advertisingText.salinity to "${sensorData.salinity.takeIf { it.isNotEmpty() } ?: "0"} mg/L"
//                )
//                is BluetoothScanViewModel.SensorData.TempLoggerData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.temperature to "${sensorData.temperature}°C",
//                    advertisingText.humidity to "${sensorData.humidity}%",
//                    advertisingText.rawData to sensorData.rawData
//                )
//                is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    advertisingText.ammonia to sensorData.ammonia,
//                    advertisingText.rawData to sensorData.rawData
//                )
//                is BluetoothScanViewModel.SensorData.DataLoggerData -> listOf(
//                    "Device ID" to sensorData.deviceId,
//                    "Total Stored Packets" to "${sensorData.currentPacketId}",
//                    "Current Received Packet ID" to "${sensorData.lastPacketId}",
//                    "Accel Points in Packet" to "${sensorData.payloadAccel.size}",
//                    "Packet Receive Time" to SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss", Locale.getDefault())
//                        .format(Date(sensorData.timestamp)),
//                    advertisingText.rawData to sensorData.rawData
//                )
//                else -> emptyList()
//            }
//        }
//    }
//
//    // Theme-aware background gradient
//    val backgroundGradient = if (isDarkMode) {
//        Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color(0xFF424242)))
//    } else {
//        Brush.verticalGradient(listOf(Color(0xFF0A74DA), Color(0xFFADD8E6)))
//    }
//
//    // Theme-aware colors
//    val cardBackground = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFF2A9EE5)
//    val textColor = if (isDarkMode) Color.White else Color.White
//    val buttonColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF0A74DA)
//
//    // Clean up resources when navigating away
//    DisposableEffect(navController) {
//        onDispose {
//            viewModel.stopScan()
//            viewModel.clearDevices()
//        }
//    }
//
//    // Main screen layout
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(backgroundGradient)
//            .systemBarsPadding()
//            .padding(WindowInsets.systemBars.asPaddingValues()),
//        contentAlignment = Alignment.Center
//    ) {
//        // Alarm blink overlay
//        if (isAlarmActive) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Red.copy(alpha = blinkAlpha))
//            )
//        }
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//                .padding(WindowInsets.systemBars.asPaddingValues()),
//            verticalArrangement = Arrangement.SpaceEvenly,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Header with navigation and chart button
//            HeaderSection(
//                navController = navController,
//                viewModel = viewModel,
//                deviceAddress = deviceAddress,
//                advertisingText = advertisingText,
//                textColor = textColor
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Device information section
//            DeviceInfoSection(
//                deviceName = deviceName,
//                deviceAddress = deviceAddress,
//                deviceId = deviceId,
//                advertisingText = advertisingText,
//                cardBackground = cardBackground,
//                cardGradient = Brush.verticalGradient(listOf(cardBackground, cardBackground.copy(alpha = 0.6f))),
//                textColor = textColor
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // DataLogger specific display (if applicable)
//            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.DataLoggerData) {
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    val latestPacket = viewModel.latestDataLoggerPacket.collectAsState().value
//
//                    if (latestPacket != null) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            InfoCard(
//                                text = "Total Packets\n${latestPacket.currentPacketId}",
//                                cardBackground = cardBackground,
//                                cardGradient = Brush.verticalGradient(listOf(cardBackground, cardBackground.copy(alpha = 0.8f))),
//                                textColor = textColor
//                            )
//                            InfoCard(
//                                text = "Current Received ID\n${latestPacket.lastPacketId}",
//                                cardBackground = cardBackground,
//                                cardGradient = Brush.verticalGradient(listOf(cardBackground, cardBackground.copy(alpha = 0.8f))),
//                                textColor = textColor
//                            )
//                            InfoCard(
//                                text = "Accel Points\n${latestPacket.payloadAccel.size}",
//                                cardBackground = cardBackground,
//                                cardGradient = Brush.verticalGradient(listOf(cardBackground, cardBackground.copy(alpha = 0.8f))),
//                                textColor = textColor
//                            )
//                            InfoCard(
//                                text = "Receive Time\n${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(latestPacket.timestamp))}",
//                                cardBackground = cardBackground,
//                                cardGradient = Brush.verticalGradient(listOf(cardBackground, cardBackground.copy(alpha = 0.8f))),
//                                textColor = textColor
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
//
//                    // Full History with XYZ + Raw
//                    DataLoggerDisplay(viewModel = viewModel)
//                }
//            }
//
//            // Responsive data cards for sensor readings
//            ResponsiveDataCards(
//                data = displayData,
//                cardBackground = cardBackground,
//                advertisingText = advertisingText,
//                textColor = textColor
//            )
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // TempLogger specific display (if applicable)
//            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.TempLoggerData) {
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(600.dp)
//                        .padding(horizontal = 16.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F8FF)
//                    ),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
//                ) {
//                    TempLoggerDisplay(
//                        viewModel = viewModel,
//                        deviceAddress = deviceAddress,
//                        deviceId = deviceId,
//                        deviceName = deviceName
//                    )
//                }
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//
//            // Threshold input section for supported sensor types
//            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.SHT40Data ||
//                currentDevice?.sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData
//            ) {
//                ThresholdInputSection(
//                    thresholdValue = thresholdValue,
//                    onThresholdChange = { thresholdValue = it },
//                    parameterType = parameterType,
//                    onParameterChange = { parameterType = it },
//                    isDarkMode = isDarkMode,
//                    sensorData = currentDevice?.sensorData,
//                    onConfirmThreshold = {
//                        if (thresholdValue.toFloatOrNull() != null) {
//                            isThresholdSet = true
//                        }
//                    }
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//            }
//
//            // Data download button
//            DownloadButton(
//                viewModel = viewModel,
//                deviceAddress = deviceAddress,
//                deviceName = deviceName,
//                deviceId = deviceId,
//                advertisingText = advertisingText
//            )
//
//            // Alarm alert dialog
//            if (showAlertDialog) {
//                AlertDialog(
//                    onDismissRequest = {
//                        showAlertDialog = false
//                        isAlarmActive = false
//                        isThresholdSet = false
//                        try {
//                            mediaPlayer?.stop()
//                            mediaPlayer?.prepare()
//                        } catch (e: IllegalStateException) {
//                            mediaPlayer?.reset()
//                            MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
//                                mediaPlayer?.release()
//                                mediaPlayer = it
//                            }
//                        }
//                    },
//                    title = { Text(advertisingText.warningTitle) },
//                    text = {
//                        Text(
//                            text = advertisingText.warningMessage.format(
//                                parameterType,
//                                thresholdValue
//                            )
//                        )
//                    },
//                    confirmButton = {
//                        Button(
//                            onClick = {
//                                showAlertDialog = false
//                                isAlarmActive = false
//                                isThresholdSet = false
//                                try {
//                                    mediaPlayer?.stop()
//                                    mediaPlayer?.prepare()
//                                } catch (e: IllegalStateException) {
//                                    mediaPlayer?.reset()
//                                    MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
//                                        mediaPlayer?.release()
//                                        mediaPlayer = it
//                                    }
//                                }
//                            }
//                        ) {
//                            Text(advertisingText.dismissButton)
//                        }
//                    }
//                )
//            }
//        }
//    }
//}
//
///**
// * Composable to display ammonia sensor data with an animated ring visualization.
// *
// * @param ammoniaValue Current ammonia concentration in ppm
// * @param modifier Compose modifier for styling
// */
//@Composable
//fun AmmoniaSensorDisplay(
//    ammoniaValue: Float,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        AmmoniaRingAnimation(ammoniaValue = ammoniaValue)
//        Text(
//            text = "%.1f ppm".format(ammoniaValue),
//            style = MaterialTheme.typography.displayMedium,
//            color = when {
//                ammoniaValue > 50 -> Red
//                ammoniaValue > 25 -> Color.Yellow
//                else -> Color.Green
//            }
//        )
//    }
//}
//
///**
// * Animated ring visualization for ammonia sensor data.
// * Shows a circular progress indicator with color-coded fill based on concentration.
// *
// * @param ammoniaValue Ammonia concentration in ppm (0-100)
// * @param modifier Compose modifier for styling
// */
//
//
///**
// * Composable to display lux sensor data with an animated ring visualization.
// *
// * @param luxValue Current light intensity in LUX
// * @param modifier Compose modifier for styling
// */
//@Composable
//fun LuxSensorDisplay(
//    luxValue: Float,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        LuxRingAnimation(luxValue = luxValue)
//        Text(
//            text = "%.0f LUX".format(luxValue),
//            style = MaterialTheme.typography.displayMedium,
//            color = when {
//                luxValue > 10000 -> Red
//                luxValue > 5000 -> Color.Yellow
//                else -> Color.Green
//            }
//        )
//    }
//}
//
///**
// * Animated ring visualization for lux sensor data.
// * Shows a circular progress indicator with color-coded fill based on light intensity.
// *
// * @param luxValue Light intensity in LUX (0-20000)
// * @param modifier Compose modifier for styling
// */
//@Composable
//fun LuxRingAnimation(
//    luxValue: Float,
//    modifier: Modifier = Modifier
//) {
//    // Animated fill percentage (0-1)
//    val animatedFill by animateFloatAsState(
//        targetValue = (luxValue / 20000f).coerceIn(0f, 1f),
//        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
//        label = "luxFill"
//    )
//
//    // Color based on light intensity
//    val lightColor by animateColorAsState(
//        targetValue = when {
//            luxValue > 10000 -> Color(0xFFF44336)  // Red (very bright)
//            luxValue > 5000 -> Color(0xFFFFC107)   // Yellow (bright)
//            else -> Color(0xFF4CAF50)              // Green (normal)
//        },
//        animationSpec = tween(durationMillis = 300),
//        label = "lightColor"
//    )
//
//    Box(
//        modifier = modifier
//            .size(220.dp)
//            .padding(16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val center = Offset(size.width / 2, size.height / 2)
//            val radius = size.minDimension * 0.4f
//            val ringWidth = size.minDimension * 0.1f
//
//            // Background ring
//            drawArc(
//                color = Color(0xFF333333).copy(alpha = 0.3f),
//                startAngle = 270f,
//                sweepAngle = 360f,
//                useCenter = false,
//                size = Size(radius * 2, radius * 2),
//                topLeft = Offset(center.x - radius, center.y - radius),
//                style = Stroke(width = ringWidth)
//            )
//
//            // Foreground fill ring
//            drawArc(
//                color = lightColor.copy(alpha = 0.7f),
//                startAngle = 270f,
//                sweepAngle = -360f * animatedFill,
//                useCenter = false,
//                size = Size(radius * 2, radius * 2),
//                topLeft = Offset(center.x - radius, center.y - radius),
//                style = Stroke(width = ringWidth, cap = StrokeCap.Round)
//            )
//        }
//
//        // Center text display
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(
//                text = "%.0f".format(luxValue),
//                fontSize = 32.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.White
//            )
//            Text(
//                text = "LUX",
//                fontSize = 16.sp,
//                color = Color.White.copy(alpha = 0.8f)
//            )
//        }
//    }
//}
//
//@Composable
//fun TempLoggerDisplay(
//    viewModel: BluetoothScanViewModel<Any>,
//    deviceAddress: String,
//    deviceId: String,
//    deviceName: String
//) {
//    // Unique identifier for this device
//    val uniqueDeviceId = deviceAddress
//
//    // Debug logging
//    LaunchedEffect(Unit) {
//        println("🔍 TEMPLOGGER DEBUG:")
//        println("   Device Name: $deviceName")
//        println("   Device Address: $deviceAddress")
//        println("   Device ID: $deviceId")
//        println("   Unique ID: $uniqueDeviceId")
//
//        val allPacketsMap = viewModel.tempLoggerPacketHistory.value
//        println("   Total Devices in ViewModel: ${allPacketsMap.keys.size}")
//        println("   All Unique IDs: ${allPacketsMap.keys}")
//
//        val devicePackets = allPacketsMap[uniqueDeviceId] ?: emptyList()
//        println("   Packets for THIS device: ${devicePackets.size}")
//    }
//
//    // Collect packet history
//    val allPacketsMap by viewModel.tempLoggerPacketHistory.collectAsState()
//    val allLatestPacketsMap by viewModel.latestTempLoggerPacket.collectAsState()
//
//    // Get packets specific to this device
//    val deviceSpecificPackets = remember(allPacketsMap, uniqueDeviceId) {
//        allPacketsMap[uniqueDeviceId] ?: emptyList()
//    }
//
//    val latestPacketForThisDevice = remember(allLatestPacketsMap, uniqueDeviceId) {
//        allLatestPacketsMap[uniqueDeviceId]
//    }
//
//    // Filter for large packets (224 bytes)
//    val largePackets = remember(deviceSpecificPackets) {
//        val filtered = deviceSpecificPackets.filter { packet ->
//            val byteCount = packet.rawData.split(" ")
//                .filter { it.isNotBlank() }
//                .count { it.trim().isNotEmpty() && it != " " }
//
//            println("📦 Packet ${packet.deviceId}: $byteCount bytes")
//
//            byteCount >= 224
//        }
//        println("📦 Total packets: ${deviceSpecificPackets.size}, Large: ${filtered.size}")
//        filtered
//    }
//
//    // Get latest large packet
//    val latestLargePacket = remember(latestPacketForThisDevice) {
//        if (latestPacketForThisDevice != null) {
//            val byteCount = latestPacketForThisDevice.rawData.split(" ")
//                .filter { it.isNotBlank() }
//                .count { it.trim().isNotEmpty() && it != " " }
//            if (byteCount >= 224) latestPacketForThisDevice else null
//        } else {
//            null
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp)
//    ) {
//        // Header with device info and live badge
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "📦 Device ${deviceAddress.takeLast(8)} Large Packets (${largePackets.size})",
//                color = Color.Black,
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp
//            )
//
//            if (latestLargePacket != null) {
//                Badge(
//                    containerColor = Color(0xFF4CAF50),
//                    contentColor = Color.White
//                ) {
//                    Text(
//                        text = "Live",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//        }
//
//        // Device info subtitle
//        Text(
//            text = "Device ID: $deviceId | Address: ${deviceAddress.takeLast(8)}",
//            color = Color.Gray,
//            fontSize = 12.sp
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        // Empty state
//        if (largePackets.isEmpty()) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .systemBarsPadding()
//                    .height(100.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(
//                        text = "No large data packets (224 bytes) received for device ${deviceAddress.takeLast(8)}",
//                        color = Color.Gray,
//                        fontSize = 14.sp
//                    )
//                    Text(
//                        text = "Received ${deviceSpecificPackets.size} normal packets (32 bytes)",
//                        color = Color.Gray.copy(alpha = 0.7f),
//                        fontSize = 12.sp
//                    )
//                }
//            }
//            return
//        }
//
//        // Statistics from large packets
//        val tempValues = largePackets.map { it.temperature.toFloatOrNull() ?: 0f }
//        val humValues = largePackets.map { it.humidity.toFloatOrNull() ?: 0f }
//
//        if (tempValues.isNotEmpty() && humValues.isNotEmpty()) {
//            val avgTemp = tempValues.average()
//            val avgHum = humValues.average()
//            val minTemp = tempValues.minOrNull() ?: 0f
//            val maxTemp = tempValues.maxOrNull() ?: 0f
//            val minHum = humValues.minOrNull() ?: 0f
//            val maxHum = humValues.maxOrNull() ?: 0f
//
//            // Statistics card
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = Color(0xFF1E3A8A)
//                )
//            ) {
//                Column(
//                    modifier = Modifier.padding(8.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "📈 Device ${deviceAddress.takeLast(8)} Statistics (${largePackets.size} packets)",
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 14.sp
//                        )
//                        Text(
//                            text = "Latest: ${latestLargePacket?.temperature}°C",
//                            color = Color.Cyan,
//                            fontSize = 12.sp
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(6.dp))
//
//                    // Temperature statistics
//                    Column(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text(
//                            text = "🌡️ Temperature",
//                            color = Color.Cyan,
//                            fontSize = 12.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Avg: ${String.format("%.1f", avgTemp)}°C", color = Color.White)
//                            Text("Min: ${String.format("%.1f", minTemp)}°C", color = Color(0xFF64B5F6))
//                            Text("Max: ${String.format("%.1f", maxTemp)}°C", color = Color(0xFFEF5350))
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(4.dp))
//
//                    // Humidity statistics
//                    Column(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text(
//                            text = "💧 Humidity",
//                            color = Color.Green,
//                            fontSize = 12.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Avg: ${String.format("%.1f", avgHum)}%", color = Color.White)
//                            Text("Min: ${String.format("%.1f", minHum)}%", color = Color(0xFF64B5F6))
//                            Text("Max: ${String.format("%.1f", maxHum)}%", color = Color(0xFFEF5350))
//                        }
//                    }
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // List of large packets
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxWidth()
//                .heightIn(max = 500.dp),
//            verticalArrangement = Arrangement.spacedBy(14.dp)
//        ) {
//            items(largePackets.reversed()) { packet ->
//                TempLoggerPacketCard(
//                    packet = packet,
//                    index = largePackets.indexOf(packet) + 1,
//                    isLatest = packet == latestLargePacket,
//                    deviceName = deviceName
//                )
//            }
//        }
//    }
//}
//
//
// * Composable for displaying individual TempLogger packet cards.
// * Shows detailed information about each packet including raw data and parsed values.
// *
// * @param packet TempLogger data packet
// * @param index Packet index in the list
// * @param isLatest Whether this is the most recent packet
// * @param deviceName Human-readable device name
// */
//@Composable
//private fun TempLoggerPacketCard(
//    packet: BluetoothScanViewModel.SensorData.TempLoggerData,
//    index: Int,
//    isLatest: Boolean,
//    deviceName: String
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isLatest) Color(0xFF2C3E50) else Color(0xFF2D3748)
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = if (isLatest) 6.dp else 4.dp
//        ),
//        border = if (isLatest) CardDefaults.outlinedCardBorder() else null
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            // Header with device name, packet number and latest badge
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text(
//                        text = "$deviceName - Packet #$index",
//                        color = Color(0xFF63B3ED),
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 16.sp
//                    )
//                    if (isLatest) {
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Badge(
//                            containerColor = Color(0xFF4CAF50),
//                            contentColor = Color.White
//                        ) {
//                            Text("LATEST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
//                        }
//                    }
//                }
//                Text(
//                    text = "Device ${packet.deviceId}",
//                    color = Color.Gray,
//                    fontSize = 12.sp
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Expandable raw data section
//            var expanded by remember { mutableStateOf(false) }
//            var showByteGroups by remember { mutableStateOf(true) }
//
//            // Calculate actual byte count
//            val actualByteCount = remember(packet.rawData) {
//                packet.rawData.split(" ")
//                    .filter { it.isNotBlank() }
//                    .count { it.trim().isNotEmpty() && it != " " }
//            }
//
//            Surface(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { expanded = !expanded },
//                color = Color(0xFF1A202C),
//                shape = MaterialTheme.shapes.small
//            ) {
//                Column(
//                    modifier = Modifier.padding(12.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = if (expanded) "📄 Hide Raw Data" else "📄 Show Raw Data",
//                            color = Color(0xFFCBD5E0),
//                            fontSize = 12.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//
//                        Text(
//                            text = if (actualByteCount >= 224) "224 bytes (7×32)" else "$actualByteCount bytes",
//                            color = Color(0xFFCBD5E0).copy(alpha = 0.6f),
//                            fontSize = 11.sp
//                        )
//                    }
//
//                    if (expanded) {
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        // Toggle between raw hex and byte groups
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceEvenly
//                        ) {
//                            FilterChip(
//                                selected = !showByteGroups,
//                                onClick = { showByteGroups = false },
//                                label = { Text("Raw Hex", fontSize = 10.sp) },
//                                colors = FilterChipDefaults.filterChipColors(
//                                    selectedContainerColor = Color(0xFF3182CE),
//                                    selectedLabelColor = Color.White
//                                )
//                            )
//
//                            FilterChip(
//                                selected = showByteGroups,
//                                onClick = { showByteGroups = true },
//                                label = { Text("32-Byte Groups", fontSize = 10.sp) },
//                                colors = FilterChipDefaults.filterChipColors(
//                                    selectedContainerColor = Color(0xFF3182CE),
//                                    selectedLabelColor = Color.White
//                                )
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        if (!showByteGroups) {
//                            // Show raw hex data
//                            Text(
//                                text = packet.rawData,
//                                color = Color(0xFFA0AEC0),
//                                fontSize = 11.sp,
//                                fontFamily = FontFamily.Monospace,
//                                lineHeight = 16.sp
//                            )
//                        } else {
//                            // Show parsed byte groups
//                            val byteGroups = parseTempLoggerRawDataIntoByteGroups(packet.rawData)
//
//                            Text(
//                                text = if (actualByteCount <= 32)
//                                    "$actualByteCount bytes (real data)"
//                                else
//                                    "$actualByteCount bytes in ${byteGroups.count { group -> group.any { it != "--" } }} groups",
//                                color = Color(0xFFCBD5E0),
//                                fontSize = 10.sp,
//                                fontStyle = FontStyle.Italic,
//                                modifier = Modifier.padding(bottom = 8.dp)
//                            )
//
//                            // Display byte groups in a lazy column
//                            LazyColumn(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .heightIn(max = 350.dp),
//                                verticalArrangement = Arrangement.spacedBy(8.dp)
//                            ) {
//                                itemsIndexed(byteGroups) { index, group ->
//                                    TempLoggerByteGroupItem(
//                                        groupNumber = index + 1,
//                                        bytes = group,
//                                        modifier = Modifier.fillMaxWidth()
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//
// * Parses TempLogger raw data into 32-byte groups.
// * Skips empty groups (all FF bytes) and handles padding.
// *
// * @param rawData Raw hex string from TempLogger
// * @return List of 32-byte groups (max 7 groups)
// */
//private fun parseTempLoggerRawDataIntoByteGroups(rawData: String?): List<List<String>> {
//    // Handle null or empty input
//    if (rawData.isNullOrBlank()) {
//        return createEmptyGroupsWithDashes()
//    }
//
//    try {
//        // Step 1: Clean and convert raw hex string to byte list
//        val bytes = rawData.split(" ")
//            .filter { it.isNotBlank() }
//            .map { it.trim() }
//            .filter { it.isNotEmpty() }
//
//        val result = mutableListOf<List<String>>()
//
//        // Step 2: Group into chunks of 32 bytes
//        val groups = bytes.chunked(32)
//
//        // Step 3: Process groups - SKIP ALL-FF GROUPS COMPLETELY
//        for (chunk in groups) {
//            // Check if this group has ONLY "FF" bytes (completely empty)
//            val isAllFF = chunk.all { it.equals("FF", ignoreCase = true) }
//
//            // Skip groups with all FF bytes
//            if (isAllFF) {
//                continue
//            }
//
//            // Check if this group has any real data (not "00" or "FF")
//            val hasRealData = chunk.any {
//                it != "00" && !it.equals("FF", ignoreCase = true) && it.isNotEmpty()
//            }
//
//            // If group has real data, add it
//            if (hasRealData) {
//                val paddedGroup = if (chunk.size < 32) {
//                    chunk.toMutableList().apply {
//                        while (size < 32) add("00")
//                    }
//                } else {
//                    chunk.toMutableList()
//                }
//                result.add(paddedGroup.take(32))
//            } else {
//                // Group with mix of 00 and FF (but not all FF)
//                val displayGroup = List(32) { index ->
//                    if (index < chunk.size) {
//                        val byte = chunk[index]
//                        if (byte.equals("FF", ignoreCase = true)) "--" else byte
//                    } else {
//                        "--"
//                    }
//                }
//                result.add(displayGroup)
//            }
//
//            // Stop after 7 valid groups
//            if (result.size >= 7) break
//        }
//
//        // Step 4: If no valid groups found, return empty
//        if (result.isEmpty()) {
//            return createEmptyGroupsWithDashes()
//        }
//
//        // Step 5: Fill remaining slots with empty groups
//        while (result.size < 7) {
//            result.add(List(32) { "--" })
//        }
//
//        return result.take(7)
//
//    } catch (e: Exception) {
//        e.printStackTrace()
//        return createEmptyGroupsWithDashes()
//    }
//}
//
///**
// * Creates empty groups with "--" for all bytes.
// * Used as fallback when no valid data is available.
// *
// * @return List of 7 empty groups with 32 "--" placeholders each
// */
//private fun createEmptyGroupsWithDashes(): List<List<String>> {
//    return List(7) { List(32) { "--" } }
//}
//
///**
// * Creates empty groups with placeholder values.
// * Legacy function for backward compatibility.
// *
// * @return List of 7 groups with sequence numbers
// */
//private fun createEmptyGroups(): List<List<String>> {
//    return List(7) { index ->
//        List(32) { byteIndex ->
//            when (byteIndex) {
//                31 -> String.format("%02X", index + 1) // Sequence number at end
//                else -> "--"
//            }
//        }
//    }
//}
//
///**
// * Extracts temperature and humidity from a 32-byte group.
// * Parses the first 4 bytes according to TempLogger protocol.
// *
// * @param bytes List of hex strings representing bytes
// * @return Pair of (temperature, humidity) strings
// */
//private fun extractTempHumidityFromGroup(bytes: List<String>): Pair<String, String> {
//    if (bytes.size < 4 || bytes.any { it == "--" }) {
//        return ("--" to "--")
//    }
//
//    try {
//        // For large packets: Bytes 1-4 contain Temp and Humidity
//        val byte1 = bytes[0].toIntOrNull(16) ?: 0  // Hex → Decimal
//        val byte2 = bytes[1].toIntOrNull(16) ?: 0  // Already Decimal
//        val byte3 = bytes[2].toIntOrNull(16) ?: 0  // Hex → Decimal
//        val byte4 = bytes[3].toIntOrNull(16) ?: 0  // Already Decimal
//
//        // If conversion fails, try hex fallback
//        val b2 = if (byte2 == 0 && bytes[1] != "00") bytes[1].toIntOrNull(16) ?: 0 else byte2
//        val b4 = if (byte4 == 0 && bytes[3] != "00") bytes[3].toIntOrNull(16) ?: 0 else byte4
//
//        // Calculate values (integer part + decimal part/100)
//        val temp = byte1 + b2 / 100.0
//        val humidity = byte3 + b4 / 100.0
//
//        return ("${String.format("%.2f", temp)}°C" to "${String.format("%.2f", humidity)}%")
//
//    } catch (e: Exception) {
//        return ("--" to "--")
//    }
//}
//
///**
// * Composable for displaying a single byte group for TempLogger.
// * Shows 32 bytes in a grid with color coding and sequence information.
// *
// * @param groupNumber Group index (1-7)
// * @param bytes List of hex strings for the 32 bytes
// * @param modifier Compose modifier for styling
// */
//@Composable
//fun TempLoggerByteGroupItem(
//    groupNumber: Int,
//    bytes: List<String>,
//    modifier: Modifier = Modifier
//) {
//    val displayBytes = bytes.take(32)
//
//    // Determine group status
//    val hasValidData = displayBytes.any { it != "00" && it != "--" }
//    val isEmptyGroup = displayBytes.all { it == "--" }
//
//    // Extract temperature and humidity if available
//    val (temperature, humidity) = remember(displayBytes) {
//        extractTempHumidityFromGroup(displayBytes)
//    }
//
//    Surface(
//        modifier = modifier,
//        shape = RoundedCornerShape(8.dp),
//        color = if (hasValidData) Color(0xFF2D3748) else Color(0xFF1A202C),
//        border = BorderStroke(1.dp, if (hasValidData) Color(0xFF4A5568) else Color(0xFF2D3748)),
//        shadowElevation = 4.dp
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Group header with temperature/humidity display
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column {
//                    Text(
//                        text = if (isEmptyGroup) "Group $groupNumber (Empty)" else "Group $groupNumber",
//                        color = if (hasValidData) Color(0xFF63B3ED) else Color(0xFF718096),
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    // Show temperature and humidity if available
//                    if (hasValidData && temperature != "--" && humidity != "--") {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(12.dp),
//                            modifier = Modifier.padding(top = 4.dp)
//                        ) {
//                            // Temperature with emoji
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(4.dp)
//                            ) {
//                                Text(
//                                    text = "🌡️",
//                                    fontSize = 14.sp
//                                )
//                                Text(
//                                    text = temperature,
//                                    color = Color(0xFFF56565),
//                                    fontSize = 12.sp,
//                                    fontWeight = FontWeight.Bold
//                                )
//                            }
//
//                            // Divider
//                            Box(
//                                modifier = Modifier
//                                    .height(12.dp)
//                                    .width(1.dp)
//                                    .background(Color(0xFF4A5568))
//                            )
//
//                            // Humidity with emoji
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(4.dp)
//                            ) {
//                                Text(
//                                    text = "💧",
//                                    fontSize = 14.sp
//                                )
//                                Text(
//                                    text = humidity,
//                                    color = Color(0xFF68D391),
//                                    fontSize = 12.sp,
//                                    fontWeight = FontWeight.Bold
//                                )
//                            }
//                        }
//                    } else if (!isEmptyGroup) {
//                        Text(
//                            text = "Bytes ${(groupNumber-1)*32+1}–${groupNumber*32}",
//                            color = Color(0xFFCBD5E0).copy(alpha = if (hasValidData) 1f else 0.5f),
//                            fontSize = 10.sp
//                        )
//                    } else {
//                        Text(
//                            text = "No data received",
//                            color = Color(0xFFCBD5E0).copy(alpha = 0.5f),
//                            fontSize = 10.sp
//                        )
//                    }
//                }
//
//                // Sequence number badge
//                val potentialSeqNum = displayBytes.getOrNull(31)
//                val seqNum = potentialSeqNum?.toIntOrNull(16) ?: groupNumber
//
//                if (!isEmptyGroup) {
//                    Surface(
//                        shape = CircleShape,
//                        color = if (seqNum == groupNumber) Color(0xFF4CAF50).copy(alpha = 0.2f)
//                        else Color(0xFFFF9800).copy(alpha = 0.2f),
//                        border = BorderStroke(
//                            1.dp,
//                            if (seqNum == groupNumber) Color(0xFF4CAF50) else Color(0xFFFF9800)
//                        )
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                        ) {
//                            Text(
//                                text = "Seq:",
//                                color = if (seqNum == groupNumber) Color(0xFF4CAF50) else Color(0xFFFF9800),
//                                fontSize = 10.sp,
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text(
//                                text = String.format("%02X", seqNum),
//                                color = if (seqNum == groupNumber) Color(0xFF4CAF50) else Color(0xFFFF9800),
//                                fontSize = 12.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            // Display bytes in grid if there's valid data
//            if (hasValidData) {
//                // Show header bytes info
//                val headerBytes = displayBytes.take(4)
//                if (headerBytes.any { it != "00" && it != "--" }) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Header: ",
//                            color = Color(0xFF63B3ED).copy(alpha = 0.7f),
//                            fontSize = 10.sp
//                        )
//                        Text(
//                            text = headerBytes.joinToString(" "),
//                            color = Color(0xFF63B3ED),
//                            fontSize = 10.sp,
//                            fontWeight = FontWeight.Bold,
//                            fontFamily = FontFamily.Monospace
//                        )
//                    }
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//
//                // Bytes grid (8 columns × 4 rows)
//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(8),
//                    modifier = Modifier.height(140.dp),
//                    verticalArrangement = Arrangement.spacedBy(4.dp),
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    itemsIndexed(displayBytes) { index, byte ->
//                        val byteNumber = index + 1
//                        val isHeaderByte = index < 4 && byte != "00" && byte != "--"
//                        val isSequenceByte = index == 31
//
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            verticalArrangement = Arrangement.Center,
//                            modifier = Modifier
//                                .background(
//                                    color = when {
//                                        isHeaderByte -> Color(0xFF63B3ED).copy(alpha = 0.1f)
//                                        isSequenceByte -> Color(0xFF4CAF50).copy(alpha = 0.1f)
//                                        else -> Color.Transparent
//                                    },
//                                    shape = RoundedCornerShape(4.dp)
//                                )
//                                .border(
//                                    width = 1.dp,
//                                    color = when {
//                                        isHeaderByte -> Color(0xFF63B3ED).copy(alpha = 0.3f)
//                                        isSequenceByte -> Color(0xFF4CAF50).copy(alpha = 0.3f)
//                                        else -> Color(0xFF4A5568).copy(alpha = 0.3f)
//                                    },
//                                    shape = RoundedCornerShape(4.dp)
//                                )
//                                .padding(4.dp)
//                        ) {
//                            // Byte number label
//                            Text(
//                                text = "B$byteNumber",
//                                color = when {
//                                    isHeaderByte -> Color(0xFF63B3ED)
//                                    isSequenceByte -> Color(0xFF4CAF50)
//                                    else -> Color(0xFFCBD5E0).copy(alpha = 0.6f)
//                                },
//                                fontSize = 8.sp,
//                                lineHeight = 9.sp
//                            )
//
//                            // Byte value
//                            Text(
//                                text = byte,
//                                color = when {
//                                    byte == "--" -> Color.Gray
//                                    isHeaderByte -> Color(0xFF63B3ED)
//                                    isSequenceByte -> Color(0xFF4CAF50)
//                                    byte == "00" -> Color(0xFF888888)
//                                    else -> Color(0xFF00FF88)
//                                },
//                                fontSize = 12.sp,
//                                fontWeight = if (isHeaderByte || isSequenceByte) FontWeight.Bold else FontWeight.Normal,
//                                lineHeight = 14.sp
//                            )
//                        }
//                    }
//                }
//            } else {
//                // Empty group message
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(80.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "No data in this group",
//                        color = Color(0xFF718096),
//                        fontSize = 12.sp,
//                        fontStyle = FontStyle.Italic
//                    )
//                }
//            }
//        }
//    }
//}
