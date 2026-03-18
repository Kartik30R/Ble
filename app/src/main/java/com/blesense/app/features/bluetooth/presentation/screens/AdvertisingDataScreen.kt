package com.blesense.app.Presentation

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
 import com.blesense.app.Presentation.widgets.HeaderSection
import com.blesense.app.R
import com.blesense.app.app.Routes

import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.coreui.theme.ThemeManager
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
    viewModel: BluetoothScanViewModel // Using the new Clean Arch ViewModel
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
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val currentDevice by remember(devices, deviceAddress) {
        derivedStateOf { devices.find { it.address == deviceAddress } }
    }
    val showGraphButton =
        currentDevice?.sensorData !is SensorData.SoilSensorData

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
                    "Packet Receive Time" to SimpleDateFormat(
                        "yyyy-MM-dd\nHH:mm:ss",
                        Locale.getDefault()
                    )
                        .format(Date(sensorData.timestamp)),
                    AppStrings.RAW_DATA to sensorData.rawData
                )

                else -> emptyList()
            }
        }
    }
    // --- UI Layout ---
    val backgroundBrush = if (isDarkMode) {
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF0A74DA), Color(0xFFADD8E6)))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HeaderSection(
                navController = navController,
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                showGraphButton = showGraphButton
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    DownloadButton(
                        viewModel = viewModel,
                        deviceAddress = deviceAddress,
                        deviceName = deviceName,
                        deviceId = deviceId
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Alarm Overlay (unchanged logic)
            if (isAlarmActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = blinkAlpha)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {



                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        DeviceInfoSection(
                            deviceName = deviceName,
                            deviceAddress = deviceAddress,
                            deviceId = deviceId,
                        )
                    }
                }

                // DataLogger Section
                if (currentDevice?.sensorData is SensorData.DataLoggerData) {
                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            DataLoggerDisplay(viewModel = viewModel)
                        }
                    }
                }

                // Sensor Data Section
                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        ResponsiveDataCards(
                            data = displayData,
                        )
                    }
                }

                // TempLogger Section
                if (currentDevice?.sensorData is SensorData.TempLoggerData) {
                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
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

                // Threshold Section
                if (currentDevice?.sensorData is SensorData.SHT40Data ||
                    currentDevice?.sensorData is SensorData.AmmoniaSensorData) {

                    ElevatedCard(
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
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


            }

            // Alert Dialog (UNCHANGED LOGIC)
            if (showAlertDialog) {
                AlertDialog(
                    onDismissRequest = {
                        isThresholdSet = false
                        showAlertDialog = false
                    },
                    title = { Text(AppStrings.WARNING_TITLE) },
                    text = {
                        Text(
                            AppStrings.WARNING_MESSAGE.format(
                                parameterType,
                                thresholdValue
                            )
                        )
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