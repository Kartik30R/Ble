package com.blesense.app.features.remote.led

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.blesense.app.R
import com.blesense.app.coreui.theme.BleSenseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// Protocol constants
// ─────────────────────────────────────────────────────────────
const val SECRET_KEY_SOLID     = 0xAA.toByte()
const val SECRET_KEY_ANIMATION = 0xBB.toByte()
const val SECRET_KEY_SLIDER    = 0xCC.toByte()

// ─────────────────────────────────────────────────────────────
// LED Commands
// ─────────────────────────────────────────────────────────────
enum class LEDCommand(
    val id: Byte,
    val displayName: String,
    val color: Color,
    val description: String,
    val mode: String,
    val companyId: Int
) {
    SOLID_MAGENTA      (1, "Magenta",     Color(0xFFFF00FF), "Solid Magenta Color",          "Solid",     0x0001),
    SOLID_YELLOW       (2, "Yellow",      Color(0xFFFFFF00), "Solid Yellow Color",            "Solid",     0x0002),
    SOLID_WHITE        (3, "White",       Color(0xFFFFFFFF), "Solid White Color",             "Solid",     0x0003),
    SOLID_RED          (4, "Red",         Color(0xFFFF0000), "Solid Red Color",               "Solid",     0x0004),
    SOLID_GREEN        (5, "Green",       Color(0xFF00FF00), "Solid Green Color",             "Solid",     0x0005),
    SOLID_BLUE         (6, "Blue",        Color(0xFF0000FF), "Solid Blue Color",              "Solid",     0x0006),
    SOLID_CYAN         (7, "Cyan",        Color(0xFF00FFFF), "Solid Cyan Color",              "Solid",     0x0007),
    INDIAN_FLAG        (8, "Indian Flag", Color(0xFFFF9933), "Saffron-White-Green Flag",      "Solid",     0x0008),
    SOLID_ORANGE       (9, "Orange",      Color(0xFFFF6600), "Solid Orange Color",            "Solid",     0x0009),

    ANIM_CYLON         (1, "Cylon Scanner",   Color(0xFFFF0000), "Red scanning back and forth",  "Animation", 0x0001),
    ANIM_POLICE        (2, "Police Strobe",   Color(0xFF0000FF), "Alternating red/blue flash",   "Animation", 0x0002),
    ANIM_BREATHER      (3, "Zen Breather",    Color(0xFF800080), "Pulsing purple breath",         "Animation", 0x0003),
    ANIM_RAINBOW       (4, "Spinning Rainbow",Color(0xFFFF69B4), "Rainbow colors cycling",        "Animation", 0x0004),
    ANIM_SHOOTING_STAR (5, "Shooting Star",   Color(0xFF87CEEB), "White comet with blue trail",   "Animation", 0x0005),
}

enum class RemoteType(
    val displayName: String,
    val color: Color,
    val description: String
) {
    REMOTE_1("Colors",     Color(0xFF007AFF), "Solid RGB colors"),
    REMOTE_2("Animations", Color(0xFFBB86FC), "Dynamic patterns"),
    REMOTE_3("Slider",     Color(0xFF03DAC6), "Custom intensity")
}

data class AdvertisingHistory(
    val label: String,
    val timestamp: Long,
    val remoteType: RemoteType,
    val companyId: Int,
    val dataHex: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertisingScreen(navController: NavHostController) {
    BleSenseTheme {
        val context = LocalContext.current
        val scope   = rememberCoroutineScope()

        var isAdvertising       by remember { mutableStateOf(false) }
        var hasPermissions      by remember { mutableStateOf(checkAdvertisingPermissions(context)) }
        var selectedRemote      by remember { mutableStateOf(RemoteType.REMOTE_1) }
        var selectedCommand     by remember { mutableStateOf(LEDCommand.SOLID_RED) }
        var showPermissionDialog by remember { mutableStateOf(false) }
        var advertisingTime     by remember { mutableStateOf(0) }
        var sliderValue         by remember { mutableStateOf(128) }
        var customColor         by remember { mutableStateOf(Color.Red) }
        var lastChangedWasColor by remember { mutableStateOf(false) }
        
        var showHistorySheet    by remember { mutableStateOf(false) }
        val sheetState          = rememberModalBottomSheetState()
        val advertisingHistory  = remember { mutableStateListOf<AdvertisingHistory>() }

        val bluetoothAdvertiser = remember {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.bluetoothLeAdvertiser
        }
        var currentCallback by remember { mutableStateOf<AdvertiseCallback?>(null) }

        // 🔗 LIVE SYNC LOGIC: Automatically update advertising when values change
        LaunchedEffect(selectedCommand, sliderValue, customColor, selectedRemote) {
            if (isAdvertising) {
                delay(200) // Debounce to prevent BLE stack spam
                currentCallback?.let { stopAdvertising(bluetoothAdvertiser, it) }
                
                val cb = createAdvertiseCallback(
                    onSuccess = { /* Success state is already handled by outer state */ },
                    onFailure = { isAdvertising = false }
                )
                currentCallback = cb
                
                when (selectedRemote) {
                    RemoteType.REMOTE_1 -> startAdvertising(bluetoothAdvertiser, cb, selectedRemote, selectedCommand)
                    RemoteType.REMOTE_2 -> startAdvertising(bluetoothAdvertiser, cb, selectedRemote, selectedCommand)
                    RemoteType.REMOTE_3 -> {
                        if (lastChangedWasColor) {
                            val closest = findClosestLEDCommand(customColor)
                            startAdvertising(bluetoothAdvertiser, cb, RemoteType.REMOTE_1, closest)
                        } else {
                            sendSliderCommand(bluetoothAdvertiser, cb, sliderValue)
                        }
                    }
                }
            }
        }

        val timeString = remember(advertisingTime) {
            val minutes = advertisingTime / 60
            val seconds = advertisingTime % 60
            String.format("%02d:%02d", minutes, seconds)
        }

        val availableCommands = remember(selectedRemote) {
            when (selectedRemote) {
                RemoteType.REMOTE_1 -> LEDCommand.values().filter { it.mode == "Solid" }
                RemoteType.REMOTE_2 -> LEDCommand.values().filter { it.mode == "Animation" }
                RemoteType.REMOTE_3 -> emptyList()
            }
        }

        LaunchedEffect(selectedRemote) {
            if (availableCommands.isNotEmpty()) selectedCommand = availableCommands.first()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LED Remote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (isAdvertising) {
                                Text("TRANSMITTING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHistorySheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { advertisingHistory.clear() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                )
            },
            bottomBar = {
                val hapticPulse by animateFloatAsState(
                    targetValue = if (isAdvertising) 1.05f else 1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = ""
                )

                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(20.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = {
                                if (!hasPermissions) { showPermissionDialog = true; return@Button }
                                if (isAdvertising) {
                                    currentCallback?.let { stopAdvertising(bluetoothAdvertiser, it) }
                                    isAdvertising = false
                                    advertisingTime = 0
                                    currentCallback = null
                                } else {
                                    val cb = createAdvertiseCallback(
                                        onSuccess = {
                                            isAdvertising = true
                                            scope.launch {
                                                while (isAdvertising) { delay(1000); advertisingTime++ }
                                            }
                                            val label = if (selectedRemote == RemoteType.REMOTE_3) "Slider: $sliderValue" else selectedCommand.displayName
                                            val cId = if (selectedRemote == RemoteType.REMOTE_3) 0x0001 else selectedCommand.companyId
                                            val hex = when(selectedRemote) {
                                                RemoteType.REMOTE_1 -> "AA"
                                                RemoteType.REMOTE_2 -> "BB"
                                                RemoteType.REMOTE_3 -> "CC ${sliderValue.toString(16).padStart(2,'0').uppercase()}"
                                            }
                                            addToHistory(advertisingHistory, label, selectedRemote, cId, hex)
                                        },
                                        onFailure = { isAdvertising = false; advertisingTime = 0 }
                                    )
                                    currentCallback = cb
                                    if (selectedRemote == RemoteType.REMOTE_3) {
                                        if (lastChangedWasColor) {
                                            val closest = findClosestLEDCommand(customColor)
                                            startAdvertising(bluetoothAdvertiser, cb, RemoteType.REMOTE_1, closest)
                                        } else {
                                            sendSliderCommand(bluetoothAdvertiser, cb, sliderValue)
                                        }
                                    } else {
                                        startAdvertising(bluetoothAdvertiser, cb, selectedRemote, selectedCommand)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .scale(if (isAdvertising) hapticPulse else 1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvertising) MaterialTheme.colorScheme.error else selectedRemote.color,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Icon(
                                if (isAdvertising) Icons.Default.Stop else Icons.Default.BluetoothAudio,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isAdvertising) "STOP ADVERTISING" else "TRANSMIT COMMAND",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // 1. FIXED: Compact Status Bar
                CompactStatusCard(
                    isAdvertising = isAdvertising,
                    time = timeString,
                    commandName = when(selectedRemote) {
                        RemoteType.REMOTE_3 -> if (lastChangedWasColor) {
                            "Match: ${findClosestLEDCommand(customColor).displayName}"
                        } else "Intensity: $sliderValue"
                        else -> selectedCommand.displayName
                    },
                    accentColor = if (selectedRemote == RemoteType.REMOTE_3 && lastChangedWasColor) customColor else selectedRemote.color,
                    companyId = if (selectedRemote == RemoteType.REMOTE_3) {
                        if (lastChangedWasColor) findClosestLEDCommand(customColor).companyId else 0x0001
                    } else selectedCommand.companyId,
                    dataHex = when(selectedRemote) {
                        RemoteType.REMOTE_1 -> "AA"
                        RemoteType.REMOTE_2 -> "BB"
                        RemoteType.REMOTE_3 -> if (lastChangedWasColor) "AA (Mapped)" else "CC ${sliderValue.toString(16).padStart(2,'0').uppercase()}"
                    }
                )

                Spacer(Modifier.height(16.dp))

                // 2. FIXED: Mode Selection Tabs
                SelectionSection(
                    selectedRemote = selectedRemote,
                    onRemoteSelect = { selectedRemote = it }
                )

                Spacer(Modifier.height(16.dp))

                // 3. SCROLLABLE: Control Options
                Text(
                    text = when(selectedRemote) {
                        RemoteType.REMOTE_1 -> "Color Palette"
                        RemoteType.REMOTE_2 -> "Animation Patterns"
                        RemoteType.REMOTE_3 -> "Custom Intensity"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        when (selectedRemote) {
                            RemoteType.REMOTE_1 -> {
                                availableCommands.chunked(3).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { cmd ->
                                            ColorSwatch(
                                                modifier = Modifier.weight(1f),
                                                command = cmd,
                                                isSelected = selectedCommand == cmd,
                                                onSelect = { selectedCommand = it }
                                            )
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            RemoteType.REMOTE_2 -> {
                                availableCommands.forEach { cmd ->
                                    AnimationListItem(
                                        command = cmd,
                                        isSelected = selectedCommand == cmd,
                                        onSelect = { selectedCommand = it }
                                    )
                                }
                            }
                            RemoteType.REMOTE_3 -> {
                                IntensitySlider(
                                    value = sliderValue,
                                    onValueChange = { 
                                        sliderValue = it
                                        lastChangedWasColor = false
                                    },
                                    accentColor = selectedRemote.color
                                )
                                Spacer(Modifier.height(16.dp))
                                ColorPickerHSV(
                                    initialColor = customColor,
                                    onColorChange = { 
                                        customColor = it
                                        lastChangedWasColor = true
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
                ) {
                    EnhancedHistorySection(history = advertisingHistory.toList())
                }
            }
        }

        if (showPermissionDialog) {
            PermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onConfirm = {
                    showPermissionDialog = false
                    if (context is androidx.activity.ComponentActivity) {
                        ActivityCompat.requestPermissions(context, getAdvertisingPermissions(), 1001)
                    }
                }
            )
        }
    }
}

@Composable
fun CompactStatusCard(
    isAdvertising: Boolean,
    time: String,
    commandName: String,
    accentColor: Color,
    companyId: Int,
    dataHex: String
) {
    val containerColor = if (isAdvertising) {
        accentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // ❌ no shadow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔘 Flat Status Indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdvertising)
                            accentColor.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isAdvertising) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 📄 Text Block
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = if (isAdvertising) commandName else "Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = if (isAdvertising) "Transmitting" else "Inactive",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // ⏱ Right Info
            Column(horizontalAlignment = Alignment.End) {

                Text(
                    text = if (isAdvertising) time else "--:--",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isAdvertising)
                        "0x${companyId.toString(16).padStart(4, '0').uppercase()}"
                    else "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = if (isAdvertising)
                        dataHex
                    else "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSection(
    selectedRemote: RemoteType,
    onRemoteSelect: (RemoteType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        RemoteType.values().forEachIndexed { index, remote ->
            SegmentedButton(
                selected = selectedRemote == remote,
                onClick = { onRemoteSelect(remote) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = RemoteType.values().size),
                icon = {}
            ) {
                Text(remote.displayName, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ColorSwatch(
    modifier: Modifier = Modifier,
    command: LEDCommand,
    isSelected: Boolean,
    onSelect: (LEDCommand) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, if (isSelected) command.color else Color.Transparent, RoundedCornerShape(16.dp))
            .background(command.color.copy(alpha = 0.15f))
            .clickable { onSelect(command) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(command.color)
                    .shadow(if (isSelected) 8.dp else 0.dp, CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(cmdDisplayName(command.displayName), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun cmdDisplayName(name: String): String = if (name == "Indian Flag") "Flag" else name

@Composable
fun AnimationListItem(
    command: LEDCommand,
    isSelected: Boolean,
    onSelect: (LEDCommand) -> Unit
) {
    OutlinedCard(
        onClick = { onSelect(command) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(isSelected).copy(
            width = if (isSelected) 2.dp else 1.dp
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) command.color.copy(alpha = 0.05f) else Color.Transparent
        )
    ) {
        ListItem(
            headlineContent = { Text(command.displayName, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(command.description) },
            leadingContent = {
                Box(Modifier.size(12.dp).clip(CircleShape).background(command.color))
            },
            trailingContent = {
                RadioButton(selected = isSelected, onClick = { onSelect(command) })
            }
        )
    }
}

@Composable
fun ColorPickerHSV(
    initialColor: Color,
    onColorChange: (Color) -> Unit
) {
    var hsv by remember {
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV((initialColor.red * 255).toInt() shl 16 or ((initialColor.green * 255).toInt() shl 8) or (initialColor.blue * 255).toInt(), hsvArr)
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2]))
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Color Picker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Hue Slider
            Text("Hue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hsv.first,
                onValueChange = { 
                    hsv = Triple(it, hsv.second, hsv.third)
                    onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(it, hsv.second, hsv.third))))
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
            )

            Spacer(Modifier.height(8.dp))

            // Saturation Slider
            Text("Saturation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hsv.second,
                onValueChange = { 
                    hsv = Triple(hsv.first, it, hsv.third)
                    onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, it, hsv.third))))
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = Color.White)
            )

            Spacer(Modifier.height(8.dp))

            // Value (Brightness) Slider
            Text("Brightness", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hsv.third,
                onValueChange = { 
                    hsv = Triple(hsv.first, hsv.second, it)
                    onColorChange(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, it))))
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = Color.White)
            )
            
            Text(
                text = "RGB: ${(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))).red * 255).toInt()}, " +
                       "${(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))).green * 255).toInt()}, " +
                       "${(Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))).blue * 255).toInt()}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun IntensitySlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Intensity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "$value / 255",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor
                )
            )

            Text(
                "Manufacturer Data: 0xCC ${value.toString(16).padStart(2,'0').uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedHistorySection(history: List<AdvertisingHistory>) {
    Column {
        Text(
            "Recent Commands",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                history.take(4).forEachIndexed { index, entry ->
                    ListItem(
                        headlineContent = { Text(entry.label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        supportingContent = {
                            Text(
                                "Comp: 0x${entry.companyId.toString(16).padStart(4,'0').uppercase()} • Data: 0x${entry.dataHex}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(entry.remoteType.color))
                        }
                    )
                    if (index < history.take(4).size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
        title   = { Text("Device Access Needed") },
        text    = { Text("BLE Sense requires Bluetooth advertising permissions to communicate with your LED devices.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Allow Access") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } }
    )
}

@Composable
fun PermissionsWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GppMaybe, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.size(12.dp))
            Text("Advertising permissions required", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private fun startAdvertising(
    advertiser: BluetoothLeAdvertiser?,
    callback: AdvertiseCallback,
    remote: RemoteType,
    command: LEDCommand
) {
    try {
        if (advertiser == null) return
        val companyId = command.companyId
        val data      = if (remote == RemoteType.REMOTE_1) byteArrayOf(SECRET_KEY_SOLID) else byteArrayOf(SECRET_KEY_ANIMATION)
        advertiser.startAdvertising(buildSettings(), buildAdvData(companyId, data), callback)
    } catch (e: SecurityException) { e.printStackTrace() }
    catch (e: Exception)          { e.printStackTrace() }
}

private fun sendSliderCommand(
    advertiser: BluetoothLeAdvertiser?,
    callback: AdvertiseCallback,
    sliderValue: Int
) {
    try {
        if (advertiser == null) return
        val companyId = 0x0001
        val data      = byteArrayOf(SECRET_KEY_SLIDER, sliderValue.toByte())
        advertiser.startAdvertising(buildSettings(), buildAdvData(companyId, data), callback)
    } catch (e: SecurityException) { e.printStackTrace() }
    catch (e: Exception)          { e.printStackTrace() }
}

private fun findClosestLEDCommand(target: Color): LEDCommand {
    val commands = LEDCommand.values().filter { it.mode == "Solid" }
    return commands.minByOrNull { cmd ->
        val dr = target.red - cmd.color.red
        val dg = target.green - cmd.color.green
        val db = target.blue - cmd.color.blue
        dr * dr + dg * dg + db * db
    } ?: LEDCommand.SOLID_WHITE
}

private fun stopAdvertising(advertiser: BluetoothLeAdvertiser?, callback: AdvertiseCallback) {
    try { advertiser?.stopAdvertising(callback) }
    catch (e: SecurityException) { e.printStackTrace() }
}

private fun buildSettings(): AdvertiseSettings =
    AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(false)
        .setTimeout(0)
        .build()

private fun buildAdvData(companyId: Int, data: ByteArray): AdvertiseData =
    AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(false)
        .addManufacturerData(companyId, data)
        .build()

private fun createAdvertiseCallback(
    onSuccess: () -> Unit,
    onFailure: () -> Unit
): AdvertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) { super.onStartSuccess(settingsInEffect); onSuccess() }
    override fun onStartFailure(errorCode: Int)                      { super.onStartFailure(errorCode);        onFailure() }
}

private fun checkAdvertisingPermissions(context: Context): Boolean =
    getAdvertisingPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

private fun getAdvertisingPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
    else
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)

private fun addToHistory(
    history: MutableList<AdvertisingHistory>,
    label: String,
    remoteType: RemoteType,
    companyId: Int,
    dataHex: String
) {
    history.add(0, AdvertisingHistory(label, System.currentTimeMillis(), remoteType, companyId, dataHex))
    if (history.size > 10) history.removeAt(history.lastIndex)
}
