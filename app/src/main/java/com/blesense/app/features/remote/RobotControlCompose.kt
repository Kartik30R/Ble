@file:Suppress("DEPRECATION", "UseCompatLoadingForDrawables")

package com.blesense.app.features.remote

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.OutputStream
import java.util.UUID
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.*
import com.blesense.app.Presentation.widgets.HeaderSection
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import com.blesense.app.R
import kotlin.random.Random

// Enum to represent Bluetooth scanning states
enum class ScanState {
    IDLE, SCANNING
}

class RobotControlCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Landscape for Robot Control
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            BleSenseTheme {
                val navController = rememberNavController()
                RobotControlScreen(
                    navController = navController,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

// ================= BLUETOOTH SCANNING VIEW MODEL =================
class ClassicBluetoothViewModel : ViewModel() {
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    internal val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var receiverRegistered = false
    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        val currentDevices = _devices.value.toMutableList()
                        if (!currentDevices.contains(device)) {
                            currentDevices.add(device)
                            _devices.value = currentDevices
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _scanState.value = ScanState.IDLE
                }
            }
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    fun startScan(context: Context) {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not supported on this device"
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            _errorMessage.value = "Bluetooth is disabled"
            return
        }
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) {
            _errorMessage.value = "Bluetooth permissions required"
            return
        }
        _scanState.value = ScanState.SCANNING
        _devices.value = emptyList()
        _errorMessage.value = null
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(deviceReceiver, filter)
            receiverRegistered = true
        }
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        bluetoothAdapter!!.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan(context: Context) {
        _scanState.value = ScanState.IDLE
        bluetoothAdapter?.cancelDiscovery()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Context handle is needed for unregistering - simplified here
    }
}

// ================= ROBOT CONTROL VIEW MODEL =================
open class RobotControlViewModel : ViewModel() {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    private var outputStream: OutputStream? = null

    open fun isBluetoothConnected(): Boolean = BluetoothConnectionManager.isConnected()

    open fun sendCommand(command: String) {
        if (!isBluetoothConnected()) {
            _isConnected.value = false
            return
        }
        try {
            if (BluetoothConnectionManager.bluetoothSocket?.isConnected != true) {
                Log.e("RobotCommand", "Bluetooth not connected!")
                _isConnected.value = false
                return
            }
            if (outputStream == null) {
                outputStream = BluetoothConnectionManager.bluetoothSocket?.outputStream
                if (outputStream == null) {
                    Log.e("RobotCommand", "Failed to get output stream!")
                    return
                }
            }
            outputStream?.write(command.toByteArray())
            outputStream?.flush()
            Log.d("RobotCommand", "Command sent successfully")
        } catch (e: Exception) {
            Log.e("RobotCommand", "Failed to send command", e)
            _isConnected.value = false
            try {
                outputStream = BluetoothConnectionManager.bluetoothSocket?.outputStream
            } catch (innerEx: Exception) {
                Log.e("RobotCommand", "Failed to refresh output stream", innerEx)
            }
        }
    }

    open fun handleSensorClick(sensorName: String, onDataReceived: (String, String) -> Unit) {
        if (isBluetoothConnected()) {
            when (sensorName) {
                "Temperature Sensor" -> {
                    val rawData = generateRandomRawData()
                    val temperature = rawData[0].toInt()
                    val humidity = rawData[1].toInt()
                    val rawDisplay = "Raw Data: ${rawData.contentToString()}"
                    val allData = "Temperature: $temperature°C\nHumidity: $humidity%"
                    onDataReceived(rawDisplay, allData)
                }
                else -> onDataReceived("Raw Data: N/A", "Default Data")
            }
        }
    }

    private fun generateRandomRawData(): ByteArray {
        val temperature = Random.nextInt(20, 40).toByte()
        val humidity = Random.nextInt(40, 80).toByte()
        return byteArrayOf(temperature, humidity)
    }
}

// Fake ViewModel for preview purposes
class FakeRobotControlViewModel : RobotControlViewModel() {
    override fun isBluetoothConnected(): Boolean = true
    override fun sendCommand(command: String) {}
    override fun handleSensorClick(sensorName: String, onDataReceived: (String, String) -> Unit) {
        onDataReceived("Raw Data: [20, 40]", "Temp: 20°C, Humidity: 40%")
    }
}

// ================= DEVICE SELECTION DIALOG =================
@Composable
fun DeviceSelectionDialog(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    onDeviceSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val cardBackgroundColor = DarkGradientStart
    val textColor = TextPrimary
    val dividerColor = GlassBorderColor

    Dialog(onDismissRequest = onDismissRequest) {
        GlassCard(
            modifier = Modifier.width(320.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SELECT_NODE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MintGreenAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MintGreenAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning for devices...",
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Searching..." else "No devices found",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(devices) { device ->
                            DeviceItem(
                                device = device,
                                onClick = { onDeviceSelected(device.address) },
                                textColor = textColor
                            )
                            HorizontalDivider(color = dividerColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "DISMISS",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    NeonPillButton(
                        text = "CLOSE",
                        onClick = onDismissRequest,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        val deviceName = remember(device) {
            try {
                device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                "Unknown Device"
            }
        }
        Text(
            text = deviceName,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = textColor
        )
        Text(
            text = device.address,
            fontSize = 14.sp,
            color = if (textColor == Color.White) Color(0xFFB0B0B0) else Color.Gray
        )
    }
}

// ================= ROBOT CONTROL SCREEN =================
@Composable
fun RobotControlScreen(
    viewModel: RobotControlViewModel = viewModel(),
    navController: androidx.navigation.NavController,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val bluetoothViewModel: ClassicBluetoothViewModel = viewModel()
    val configuration = LocalConfiguration.current
    var isConnected by remember { mutableStateOf(BluetoothConnectionManager.isConnected()) }
    val isDarkMode = true 
    val backgroundColor = DarkGradientStart
    val textColor = TextPrimary
    val secondaryTextColor = TextSecondary
    val iconTint = MintGreenAccent

    var selectedSensor by remember { mutableStateOf(SensorItem(0, "Select Sensor")) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogContent by remember { mutableStateOf("") }
    var showDeviceDialog by remember { mutableStateOf(false) }
    val scanState by bluetoothViewModel.scanState.collectAsState()
    val devices by bluetoothViewModel.devices.collectAsState()
    val errorMessage by bluetoothViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            isConnected = BluetoothConnectionManager.isConnected()
            delay(1000)
        }
    }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            bluetoothViewModel.startScan(context)
            showDeviceDialog = true
        }
    }

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        if (bluetoothEnabled) {
            val hasPermissions = bluetoothPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (hasPermissions) {
                bluetoothViewModel.startScan(context)
                showDeviceDialog = true
            } else {
                permissionsLauncher.launch(bluetoothPermissions)
            }
        } else {
            Toast.makeText(context, "Bluetooth must be enabled to scan for devices", Toast.LENGTH_SHORT).show()
        }
    }

    val sensorData = listOf(
        SensorItem(0, "Select Sensor"),
        SensorItem(R.drawable.ic_thermometer, "Temperature Sensor"),
        SensorItem(R.drawable.ic_accelerometer, "Accelerometer Sensor"),
        SensorItem(R.drawable.ic_pressure_sensor, "Pressure Sensor"),
        SensorItem(R.drawable.ic_turbo, "Turbo Sensor"),
        SensorItem(R.drawable.ic_motor, "Motor Sensor"),
        SensorItem(R.drawable.ic_switch, "Switch Sensor")
    )

    val backgroundPainter = painterResource(id = R.drawable.racing_bg7)

    DisposableEffect(Unit) {
        onDispose {
            if (scanState == ScanState.SCANNING) {
                bluetoothViewModel.stopScan(context)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .neumorphicBackground()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection(
                navController = navController,
                viewModel = null, // Using the local ClassicBluetoothViewModel for discovery
                deviceAddress = "ROBOT_COMMAND_HUB"
            )

            Box(modifier = Modifier.weight(1f)) {
                // Main Control Area
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Joysticks/Sensors
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VerticalJoystick(
                            modifier = Modifier.padding(bottom = 32.dp),
                            onDirectionChange = { command ->
                                viewModel.sendCommand(command)
                            },
                            isDarkMode = isDarkMode
                        )
                        
                        SensorSpinner(
                            sensorData = sensorData,
                            selectedSensor = selectedSensor,
                            onSensorSelected = { sensor ->
                                selectedSensor = sensor
                                if (sensor.name != "Select Sensor") {
                                    viewModel.handleSensorClick(sensor.name) { rawDisplay, allData ->
                                        dialogContent = "$rawDisplay\n$allData"
                                        showDialog = true
                                    }
                                }
                            },
                            isDarkMode = isDarkMode
                        )
                    }

                    // Right Side: Action Controls
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalJoystick(
                            modifier = Modifier.padding(bottom = 32.dp),
                            onDirectionChange = { direction ->
                                when (direction) {
                                    "L" -> viewModel.sendCommand("L")
                                    "R" -> viewModel.sendCommand("R")
                                    else -> viewModel.sendCommand("C")
                                }
                            },
                            isDarkMode = isDarkMode
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            BluetoothButton(
                                onClick = {
                                    if (BluetoothConnectionManager.isConnected()) {
                                        BluetoothConnectionManager.disconnect()
                                        return@BluetoothButton
                                    }
                                    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                                    if (bluetoothAdapter == null) return@BluetoothButton
                                    if (!bluetoothAdapter.isEnabled) {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        bluetoothEnableLauncher.launch(enableBtIntent)
                                    } else {
                                        val hasPermissions = bluetoothPermissions.all {
                                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                        }
                                        if (hasPermissions) {
                                            bluetoothViewModel.startScan(context)
                                            showDeviceDialog = true
                                        } else {
                                            permissionsLauncher.launch(bluetoothPermissions)
                                        }
                                    }
                                },
                                isDarkMode = isDarkMode
                            )

                            HornButton(
                                isBluetoothConnected = isConnected,
                                onHornActive = { isActive ->
                                    if (isActive) viewModel.sendCommand("H")
                                    else viewModel.sendCommand("C")
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }

                // Connection Status Floating Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    GlassCard {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) Color.Green else Color.Red,
                                        CircleShape
                                    )
                                    .neonGlow(active = isConnected)
                            )
                            Text(
                                text = if (isConnected) "SECURE_LINK: ACTIVE" else "LINK_STATUS: OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color.Green else Color.Red,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SensorDataDialog(
            sensorName = selectedSensor.name,
            content = dialogContent,
            onDismiss = { showDialog = false },
            isDarkMode = isDarkMode
        )
    }
    if (showDeviceDialog) {
        DeviceSelectionDialog(
            devices = devices,
            isScanning = scanState == ScanState.SCANNING,
            onDeviceSelected = { address ->
                connectToDevice(context, address)
                showDeviceDialog = false
            },
            onDismissRequest = {
                showDeviceDialog = false
                bluetoothViewModel.stopScan(context)
            }
        )
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            bluetoothViewModel.clearError()
        }
    }
}

// BackButton is deprecated as HeaderSection handles navigation

@Composable
fun BluetoothButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clickable { onClick() }
            .glassCard(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = "Bluetooth",
            tint = MintGreenAccent,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun HornButton(
    modifier: Modifier = Modifier,
    isBluetoothConnected: Boolean,
    onHornActive: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isBluetoothConnected) {
            onHornActive(isPressed)
        }
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .glassCard(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isBluetoothConnected) {
                            try {
                                onHornActive(true)
                                awaitRelease()
                            } finally {
                                onHornActive(false)
                            }
                        }
                    }
                )
            }
            .then(if (isPressed) Modifier.neonGlow(active = true, cornerShape = CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_horn),
            contentDescription = "Horn",
            tint = if (isPressed) Color.Red else Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun VerticalJoystick(
    modifier: Modifier = Modifier,
    onDirectionChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxDistance = with(density) { 60.dp.toPx() }
    val deadZone = with(density) { 5.dp.toPx() }
    var currentCommand by remember { mutableStateOf("C") }
    Box(
        modifier = modifier
            .size(160.dp)
            .glassInset(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newY = (offset.y + dragAmount.y).coerceIn(-maxDistance, maxDistance)
                        val newOffset = Offset(0f, newY)
                        offset = newOffset
                        val newCommand = when {
                            newY < -deadZone -> "U"
                            newY > deadZone -> "D"
                            else -> "C"
                        }
                        if (newCommand != currentCommand) {
                            currentCommand = newCommand
                            onDirectionChange(newCommand)
                        }
                    },
                    onDragEnd = {
                        offset = Offset.Zero
                        if (currentCommand != "C") {
                            currentCommand = "C"
                            onDirectionChange("C")
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Center marker
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(MintGreenAccent.copy(alpha = 0.3f), CircleShape)
        )

        // Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(72.dp)
                .glassCard(CircleShape)
                .neonGlow(active = currentCommand != "C", cornerShape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack, // Placeholder or custom icon
                contentDescription = null,
                tint = if (currentCommand != "C") MintGreenAccent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp).rotate(if (currentCommand == "U") 90f else if (currentCommand == "D") 270f else 0f)
            )
        }
    }
}

@Composable
fun HorizontalJoystick(
    modifier: Modifier = Modifier,
    onDirectionChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    val density = LocalDensity.current
    var offset by remember { mutableStateOf(Offset.Zero) }
    val maxDistance = with(density) { 60.dp.toPx() }
    val deadZone = with(density) { 5.dp.toPx() }
    var currentCommand by remember { mutableStateOf("C") }
    Box(
        modifier = modifier
            .size(160.dp)
            .glassInset(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = (offset.x + dragAmount.x).coerceIn(-maxDistance, maxDistance)
                        val newOffset = Offset(newX, 0f)
                        offset = newOffset
                        val newCommand = when {
                            newX < -deadZone -> "L"
                            newX > deadZone -> "R"
                            else -> "C"
                        }
                        if (newCommand != currentCommand) {
                            currentCommand = newCommand
                            onDirectionChange(newCommand)
                        }
                    },
                    onDragEnd = {
                        offset = Offset.Zero
                        if (currentCommand != "C") {
                            currentCommand = "C"
                            onDirectionChange("C")
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Center marker
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(MintGreenAccent.copy(alpha = 0.3f), CircleShape)
        )

        // Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                .size(72.dp)
                .glassCard(CircleShape)
                .neonGlow(active = currentCommand != "C", cornerShape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = if (currentCommand != "C") MintGreenAccent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp).rotate(if (currentCommand == "R") 180f else 0f)
            )
        }
    }
}

@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun SensorSpinner(
    modifier: Modifier = Modifier,
    sensorData: List<SensorItem>,
    selectedSensor: SensorItem,
    onSensorSelected: (SensorItem) -> Unit,
    isDarkMode: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val backgroundColor = GlassSurfaceColor
    val textColor = TextPrimary
    val dropdownBackgroundColor = DarkGradientStart
    val dropdownTextColor = TextPrimary
    val iconColor = TextPrimary
    val context = LocalContext.current
    Box(
        modifier = modifier.width(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .glassInset(CircleShape)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (selectedSensor.iconResId != 0) {
                Icon(
                    painter = painterResource(id = selectedSensor.iconResId),
                    contentDescription = selectedSensor.name,
                    modifier = Modifier.size(20.dp),
                    tint = MintGreenAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = selectedSensor.name.replace(" Sensor", ""),
                color = TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .background(DarkGradientStart)
                .border(1.dp, GlassBorderColor, RoundedCornerShape(12.dp))
        ) {
            sensorData.forEach { sensor ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sensor.iconResId != 0) {
                                Icon(
                                    painter = painterResource(id = sensor.iconResId),
                                    contentDescription = sensor.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedSensor == sensor) MintGreenAccent else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = sensor.name,
                                color = if (selectedSensor == sensor) MintGreenAccent else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    onClick = {
                        onSensorSelected(sensor)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SensorDataDialog(
    sensorName: String,
    content: String,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val cardBackgroundColor = DarkGradientStart
    val textColor = TextPrimary
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.width(320.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = sensorName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MintGreenAccent
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = GlassBorderColor)
                
                GlassInsetBox(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    cornerShape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                NeonPillButton(
                    text = "DISMISS_TELEMETRY",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun connectToDevice(context: Context, address: String) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val device = bluetoothAdapter.getRemoteDevice(address)
    val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    BluetoothConnectionManager.disconnect()
    Thread {
        try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            try {
                socket.connect()
            } catch (connectException: Exception) {
                try {
                    socket.close()
                } catch (closeException: Exception) { }
                try {
                    Log.d("BluetoothConnect", "Trying fallback connection...")
                    val fallbackSocket = createFallbackSocket(device)
                    fallbackSocket?.connect()
                    if (fallbackSocket?.isConnected == true) {
                        BluetoothConnectionManager.bluetoothSocket = fallbackSocket
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "Connected to ${device.name} (fallback)", Toast.LENGTH_SHORT).show()
                        }
                        return@Thread
                    }
                } catch (fallbackException: Exception) {
                    Log.e("BluetoothConnect", "Fallback connection failed", fallbackException)
                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Connection failed: ${fallbackException.message}", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                throw connectException
            }
            BluetoothConnectionManager.bluetoothSocket = socket
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }.start()
}

data class SensorItem(val iconResId: Int, val name: String)

@SuppressLint("MissingPermission", "DiscouragedPrivateApi")
private fun createFallbackSocket(device: BluetoothDevice): BluetoothSocket? {
    try {
        val m = device.javaClass.getMethod(
            "createRfcommSocket",
            *arrayOf<Class<*>>(Int::class.javaPrimitiveType as Class<*>)
        )
        return m.invoke(device, 1) as BluetoothSocket
    } catch (e: Exception) {
        Log.e("BluetoothConnect", "Fallback socket creation failed", e)
    }
    return null
}

object BluetoothConnectionManager {
    var bluetoothSocket: BluetoothSocket? = null
    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error closing socket: ${e.message}")
        }
    }
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PreviewRobotControlScreen() {
    val navController = rememberNavController()
    RobotControlScreen(
        viewModel = FakeRobotControlViewModel(),
        navController = navController,
        onBackPressed = {}
    )
}