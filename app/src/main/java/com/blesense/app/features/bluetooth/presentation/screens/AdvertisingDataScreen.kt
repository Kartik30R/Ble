package com.blesense.app.Presentation

import android.net.Uri

import android.app.Activity
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.blesense.app.Presentation.widgets.HeaderSection
import com.blesense.app.R
import com.blesense.app.app.Routes

import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.*
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.presentation.widget.DataLoggerDisplay
import com.blesense.app.features.bluetooth.presentation.widget.DeviceInfoSection
import com.blesense.app.features.bluetooth.presentation.widget.DownloadButton
import com.blesense.app.features.bluetooth.presentation.widget.ResponsiveDataCards
import com.blesense.app.features.bluetooth.presentation.widget.TempLoggerDisplay
import com.blesense.app.features.bluetooth.presentation.widget.ThresholdInputSection
import presentation.viewmodel.BluetoothScanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdvertisingDataScreen(
    deviceAddress: String,
    deviceName: String,
    navController: NavController,
    deviceId: String,
    viewModel: BluetoothScanViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- Lifecycle Management ---
    LaunchedEffect(activity) {
        activity?.let { viewModel.startScan() }
    }

    // Handle Back Button and Cleanup
    BackHandler {
        viewModel.stopScan()
        viewModel.clearDevices()
        navController.popBackStack()
    }

    // --- Alarm & Media Logic ---
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    LaunchedEffect(Unit) {
        mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm)?.apply { isLooping = true }
    }

    // --- State Observation ---
    val devices by viewModel.devices.collectAsState()
    val currentDevice by remember(devices, deviceAddress) {
        derivedStateOf { devices.find { it.address == deviceAddress } }
    }
    val isGraphAvailable = currentDevice?.sensorData !is SensorData.SoilSensorData

    // Threshold & Alarm States
    var thresholdValue by remember { mutableStateOf("") }
    var isAlarmActive by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var parameterType by remember { mutableStateOf(AppStrings.TEMPERATURE) }
    var isThresholdSet by remember { mutableStateOf(false) }

    // Blinking Animation
    val infiniteTransition = rememberInfiniteTransition(label = "alarmBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAlarmActive) 0.4f else 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "blinkAlpha"
    )

    // --- Threshold Monitoring Logic ---
    LaunchedEffect(currentDevice?.sensorData, thresholdValue, isThresholdSet) {
        if (isThresholdSet) {
            val threshold = thresholdValue.toFloatOrNull() ?: return@LaunchedEffect
            val sensorData = currentDevice?.sensorData

            isAlarmActive = when (sensorData) {
                is SensorData.SHT40Data -> {
                    val value = if (parameterType == AppStrings.TEMPERATURE)
                        sensorData.temperature.toFloatOrNull()
                    else sensorData.humidity.toFloatOrNull()
                    value != null && value > threshold
                }
                is SensorData.AmmoniaSensorData -> {
                    val value = sensorData.ammonia.replace(" ppm", "").toFloatOrNull()
                    parameterType == AppStrings.AMMONIA && value != null && value > threshold
                }
                else -> false
            }

            if (isAlarmActive) {
                showAlertDialog = true
                if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
            } else {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    mediaPlayer?.seekTo(0)
                }
            }
        }
    }

    // --- Display Data Mapping ---
    val displayData by remember(currentDevice?.sensorData) {
        derivedStateOf {
            val sensorData = currentDevice?.sensorData
            when (sensorData) {
                is SensorData.SHT40Data -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.TEMPERATURE to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    AppStrings.HUMIDITY to "${sensorData.humidity.takeIf { it.isNotEmpty() } ?: "0"}%"
                )
                is SensorData.SDTData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.SPEED to "${sensorData.speed.takeIf { it.isNotEmpty() } ?: "0"} m/s",
                    AppStrings.DISTANCE to "${sensorData.distance.takeIf { it.isNotEmpty() } ?: "0"} m"
                )
                is SensorData.LIS2DHData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.X_AXIS to "${sensorData.x.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    AppStrings.Y_AXIS to "${sensorData.y.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    AppStrings.Z_AXIS to "${sensorData.z.takeIf { it.isNotEmpty() } ?: "0"} m/s²"
                )
                is SensorData.SoilSensorData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.NITROGEN to "${sensorData.nitrogen.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    AppStrings.PHOSPHORUS to "${sensorData.phosphorus.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    AppStrings.POTASSIUM to "${sensorData.potassium.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    AppStrings.MOISTURE to "${sensorData.moisture.takeIf { it.isNotEmpty() } ?: "0"}%",
                    AppStrings.TEMPERATURE to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    AppStrings.ELECTRIC_CONDUCTIVITY to "${sensorData.ec.takeIf { it.isNotEmpty() } ?: "0"} mS/cm",
                    AppStrings.PH to (sensorData.pH.takeIf { it.isNotEmpty() } ?: "0"),
                    AppStrings.SALINITY to "${sensorData.salinity.takeIf { it.isNotEmpty() } ?: "0"} mg/L"
                )
                is SensorData.LuxSensorData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.LIGHT_INTENSITY to "${sensorData.lux.takeIf { it.isNotEmpty() } ?: "0"} Lux",
                    AppStrings.RAW_DATA to sensorData.rawData
                )
                is SensorData.AmmoniaSensorData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.AMMONIA to (sensorData.ammonia.takeIf { it.isNotEmpty() } ?: "0 ppm"),
                    AppStrings.RAW_DATA to sensorData.rawData
                )
                is SensorData.TempLoggerData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.TEMPERATURE to "${sensorData.temperature}°C",
                    AppStrings.HUMIDITY to "${sensorData.humidity}%",
                    AppStrings.RAW_DATA to sensorData.rawData
                )
                is SensorData.DataLoggerData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    "Total Stored Packets" to "${sensorData.currentPacketId}",
                    "Current Received ID" to "${sensorData.lastPacketId}",
                    "Accel Points" to "${sensorData.payloadAccel.size}",
                    "Packet Receive Time" to SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss", Locale.getDefault()).format(Date(sensorData.timestamp)),
                    AppStrings.RAW_DATA to sensorData.rawData
                )
                is SensorData.Sen6xData -> listOf(
                    AppStrings.NODE_ID_LABEL to sensorData.deviceId,
                    AppStrings.PM1 to "${sensorData.pm1} µg/m³",
                    AppStrings.PM2_5 to "${sensorData.pm25} µg/m³",
                    AppStrings.PM4 to "${sensorData.pm4} µg/m³",
                    AppStrings.PM10 to "${sensorData.pm10} µg/m³",
                    AppStrings.TEMPERATURE to "${sensorData.temperature}°C",
                    AppStrings.HUMIDITY to "${sensorData.humidity}%",
                    AppStrings.CO2 to "${sensorData.co2} ppm",
                    AppStrings.VOC to (sensorData.voc.takeIf { it != "0" } ?: "0"),
                    AppStrings.NOX to (sensorData.nox.takeIf { it != "0" } ?: "0")
                )
                else -> emptyList()
            }
        }
    }

    /* --- UI Layout --- */
    Box(modifier = Modifier.fillMaxSize().neumorphicBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HeaderSection(
                    navController = navController,
                    viewModel = viewModel,
                    deviceAddress = deviceAddress
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Device Info (Neomorphic header)
                    DeviceInfoSection(
                        deviceName = deviceName,
                        deviceAddress = deviceAddress,
                        sensorData = currentDevice?.sensorData
                    )

                    // 2. Data Logger View (If applicable)
                    if (currentDevice?.sensorData is SensorData.DataLoggerData) {
                        GlassCard {
                            Column(Modifier.padding(20.dp)) {
                                DataLoggerDisplay(viewModel = viewModel)
                            }
                        }
                    }

                    // 3. Primary Data Visualization
                    ResponsiveDataCards(
                        displayData = displayData,
                        isAlarmActive = isAlarmActive,
                        blinkAlpha = blinkAlpha
                    )

                    // 3.5. Live Analytics Card (Restored)
                    if (isGraphAvailable) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Live Analytics",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Visualise real-time trends",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                
                                NeonPillButton(
                                    text = "Open Chart",
                                    onClick = {
                                        val encoded = Uri.encode(deviceAddress)
                                        navController.navigate("chart_screen/$encoded")
                                    },
                                    isActive = true
                                )
                            }
                        }
                    }

                    // 4. Temp Logger View (If applicable)
                    if (currentDevice?.sensorData is SensorData.TempLoggerData) {
                        GlassCard {
                            Column(Modifier.padding(20.dp)) {
                                TempLoggerDisplay(
                                    viewModel = viewModel,
                                    deviceAddress = deviceAddress,
                                    deviceId = deviceId,
                                    deviceName = deviceName
                                )
                            }
                        }
                    }

                    // 5. Threshold Configuration
                    if (currentDevice?.sensorData is SensorData.SHT40Data ||
                        currentDevice?.sensorData is SensorData.AmmoniaSensorData ||
                        currentDevice?.sensorData is SensorData.SoilSensorData) {
                        GlassCard {
                            Column(Modifier.padding(20.dp)) {
                                ThresholdInputSection(
                                    thresholdValue = thresholdValue,
                                    onThresholdChange = { thresholdValue = it },
                                    parameterType = parameterType,
                                    onParameterChange = { parameterType = it },
                                    sensorData = currentDevice?.sensorData,
                                    onConfirmThreshold = { isThresholdSet = true }
                                )
                            }
                        }
                    }

                    // Add padding at the bottom so content doesn't get hidden by floating buttons
                    Spacer(modifier = Modifier.height(120.dp))
                }

                // Floating Action Buttons (Flutter-style Stack)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    DownloadButton(
                        viewModel = viewModel,
                        deviceAddress = deviceAddress,
                        deviceName = deviceName,
                        deviceId = deviceId,
                        isVertical = false
                    )
                }

                // Alarm Overlay
                if (isAlarmActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.error.copy(alpha = blinkAlpha))
                    )
                }

                // ALERT DIALOG
                if (showAlertDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            isThresholdSet = false
                            showAlertDialog = false
                        },
                        title = { Text(AppStrings.WARNING_TITLE) },
                        text = {
                            Text(AppStrings.WARNING_MESSAGE.format(parameterType, thresholdValue))
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    isThresholdSet = false
                                    showAlertDialog = false
                                }
                            ) {
                                Text(AppStrings.DISMISS)
                            }
                        }
                    )
                }
            }
        }
    }
}