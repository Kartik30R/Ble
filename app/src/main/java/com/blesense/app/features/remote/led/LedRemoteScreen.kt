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
import androidx.compose.animation.*
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
import com.blesense.app.coreui.components.*
import com.blesense.app.coreui.theme.*
import com.blesense.app.Presentation.widgets.HeaderSection
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
    SOLID_OFF          (0, "OFF",         Color.Black,       "Turn LEDs off",                 "Solid",     0x0000),

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

        var isAdvActive         by remember { mutableStateOf(false) }
        var hasBleAdvPermissions by remember { mutableStateOf(checkAdvertisingPermissions(context)) }
        var selectedRemote      by remember { mutableStateOf(RemoteType.REMOTE_1) }
        var selectedCommands    by remember { mutableStateOf(setOf(LEDCommand.SOLID_RED)) }
        var cycleIntervalSeconds by remember { mutableStateOf(2) }
        var showPermissionDialog by remember { mutableStateOf(false) }
        var advertisingTime     by remember { mutableStateOf(0) }
        var sliderValue         by remember { mutableStateOf(128) }

        // 🕺 DISCO STATE
        var isDiscoEnabled      by remember { mutableStateOf(false) }
        var isStrobeEnabled     by remember { mutableStateOf(false) }
        var discoSpeedMs        by remember { mutableStateOf(300L) }
        var isDiscoRandom       by remember { mutableStateOf(false) }
        var discoCounter        by remember { mutableStateOf(0) }

        var showHistorySheet    by remember { mutableStateOf(false) }
        val sheetState          = rememberModalBottomSheetState()
        val advertisingHistory  = remember { mutableStateListOf<AdvertisingHistory>() }

        val bluetoothAdvertiser = remember {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.bluetoothLeAdvertiser
        }
        var currentCallback by remember { mutableStateOf<AdvertiseCallback?>(null) }

        // 🔄 DISCO LOOP: High-speed counter for strobe effect
        LaunchedEffect(isDiscoEnabled, discoSpeedMs) {
            if (isDiscoEnabled && isAdvActive) {
                while (true) {
                    delay(discoSpeedMs)
                    discoCounter++
                }
            }
        }

        // 🔄 CYCLING LOGIC: Derive the active command from the loop
        val activeCommand = remember(selectedCommands, advertisingTime, cycleIntervalSeconds, selectedRemote, isDiscoEnabled, discoCounter, isDiscoRandom, isStrobeEnabled) {
            val list = if (isDiscoEnabled) {
                LEDCommand.values().filter { it.mode == "Solid" }
            } else {
                selectedCommands.toList()
            }

            if (list.isEmpty()) LEDCommand.SOLID_RED
            else {
                // Strobe Mode Alternation (Every second frame is Black)
                if (isDiscoEnabled && isStrobeEnabled && (discoCounter % 2 == 1)) {
                    LEDCommand.SOLID_OFF
                } else {
                    val index = if (isDiscoEnabled) {
                        if (isDiscoRandom) (0 until list.size).random()
                        else discoCounter % list.size
                    } else {
                        (advertisingTime / cycleIntervalSeconds) % list.size
                    }
                    list[index]
                }
            }
        }

        // 🔗 LIVE SYNC LOGIC: Automatically update advertising when values change
        LaunchedEffect(activeCommand, sliderValue, selectedRemote) {
            if (isAdvActive) {
                // Adjust debounce for Disco strobe
                val debounce = if (isDiscoEnabled) (discoSpeedMs / 3).coerceAtMost(100L) else 200L
                delay(debounce)

                currentCallback?.let { stopAdvertising(bluetoothAdvertiser, it) }

                val cb = createAdvertiseCallback(
                    onSuccess = { /* Success state is already handled by outer state */ },
                    onFailure = { isAdvActive = false }
                )
                currentCallback = cb

                when {
                    isDiscoEnabled -> startAdvertising(bluetoothAdvertiser, cb, RemoteType.REMOTE_1, activeCommand)
                    selectedRemote == RemoteType.REMOTE_1 -> startAdvertising(bluetoothAdvertiser, cb, selectedRemote, activeCommand)
                    selectedRemote == RemoteType.REMOTE_2 -> startAdvertising(bluetoothAdvertiser, cb, selectedRemote, activeCommand)
                    selectedRemote == RemoteType.REMOTE_3 -> sendSliderCommand(bluetoothAdvertiser, cb, sliderValue)
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
            if (availableCommands.isNotEmpty()) {
                selectedCommands = setOf(availableCommands.first())
            }
        }

        /* ---------------- UI ---------------- */

        Box(modifier = Modifier.fillMaxSize().neumorphicBackground()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    HeaderSection(
                        navController = navController,
                        viewModel = null,
                        deviceAddress = "LED Remote"
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Scrollable Area
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. Status Bar
                        CompactStatusCard(
                            isAdvertising = isAdvActive,
                            time = timeString,
                            commandName = when {
                                isDiscoEnabled && isAdvActive -> if (activeCommand.displayName == "DARK") "STROBE: OFF" else "DISCO: ${activeCommand.displayName}"
                                selectedRemote == RemoteType.REMOTE_3 -> "Intensity: $sliderValue"
                                else -> if (selectedCommands.size > 1) "${activeCommand.displayName} (Cycle)" else activeCommand.displayName
                            },
                            accentColor = when {
                                isDiscoEnabled && isAdvActive -> activeCommand.color
                                selectedRemote == RemoteType.REMOTE_3 -> selectedRemote.color
                                else -> activeCommand.color
                            },
                            companyId = if (selectedRemote == RemoteType.REMOTE_3 && !isDiscoEnabled) 0x0001 else activeCommand.companyId,
                            dataHex = when {
                                isDiscoEnabled && isAdvActive -> "AA (Disco)"
                                selectedRemote == RemoteType.REMOTE_1 -> "AA"
                                selectedRemote == RemoteType.REMOTE_2 -> "BB"
                                selectedRemote == RemoteType.REMOTE_3 -> "CC ${sliderValue.toString(16).padStart(2,'0').uppercase()}"
                                else -> "—"
                            }
                        )

                        Spacer(Modifier.height(20.dp))

                        // 2. Mode Selection Tabs
                        SelectionSection(
                            selectedRemote = selectedRemote,
                            onRemoteSelect = { selectedRemote = it }
                        )

                        Spacer(Modifier.height(20.dp))

                        // 3. Control Options
                        Text(
                            text = when(selectedRemote) {
                                RemoteType.REMOTE_1 -> "Color Palette Control"
                                RemoteType.REMOTE_2 -> "Pattern Visualizers"
                                RemoteType.REMOTE_3 -> "Custom Luminance"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                when (selectedRemote) {
                                    RemoteType.REMOTE_1 -> {
                                        DiscoControlCard(
                                            isEnabled = isDiscoEnabled,
                                            onToggle = { isDiscoEnabled = it },
                                            speed = discoSpeedMs,
                                            onSpeedChange = { discoSpeedMs = it },
                                            isRandom = isDiscoRandom,
                                            onRandomToggle = { isDiscoRandom = it },
                                            isStrobe = isStrobeEnabled,
                                            onStrobeToggle = { isStrobeEnabled = it }
                                        )
                                        Spacer(Modifier.height(20.dp))

                                        CycleIntervalControl(
                                            interval = cycleIntervalSeconds,
                                            onIntervalChange = { cycleIntervalSeconds = it },
                                            accentColor = selectedRemote.color
                                        )
                                        Spacer(Modifier.height(20.dp))

                                        availableCommands.chunked(3).forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                row.forEach { cmd ->
                                                    ColorSwatch(
                                                        modifier = Modifier.weight(1f),
                                                        command = cmd,
                                                        isSelected = selectedCommands.contains(cmd),
                                                        onSelect = { selected ->
                                                            selectedCommands = if (selectedCommands.contains(selected)) {
                                                                if (selectedCommands.size > 1) selectedCommands.minus(selected) else selectedCommands
                                                            } else {
                                                                selectedCommands.plus(selected)
                                                            }
                                                        }
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
                                                isSelected = selectedCommands.contains(cmd),
                                                onSelect = { selected ->
                                                    selectedCommands = if (selectedCommands.contains(selected)) {
                                                        if (selectedCommands.size > 1) selectedCommands.minus(selected) else selectedCommands
                                                    } else {
                                                        selectedCommands.plus(selected)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    RemoteType.REMOTE_3 -> {
                                        IntensitySlider(
                                            value = sliderValue,
                                            onValueChange = { sliderValue = it },
                                            accentColor = activeCommand.color
                                        )
                                    }
                                }
                            }
                            item { 
                                Spacer(Modifier.height(16.dp))
                                NeonPillButton(
                                    text = "View Command Logs",
                                    onClick = { showHistorySheet = true },
                                    isActive = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    // 🕺 Floating Transmit Button Layer
                    val hapticPulse by animateFloatAsState(
                        targetValue = if (isAdvActive) 1.05f else 1f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        NeonPillButton(
                            text = if (isAdvActive) "STOP TRANSMISSION" else "TRANSMIT COMMAND",
                            onClick = {
                                if (!hasBleAdvPermissions) { showPermissionDialog = true; return@NeonPillButton }
                                if (isAdvActive) {
                                    currentCallback?.let { stopAdvertising(bluetoothAdvertiser, it) }
                                    isAdvActive = false
                                    advertisingTime = 0
                                    currentCallback = null
                                } else {
                                    val cb = createAdvertiseCallback(
                                        onSuccess = {
                                            isAdvActive = true
                                            scope.launch {
                                                while (isAdvActive) { delay(1000); advertisingTime++ }
                                            }
                                            val label = if (selectedRemote == RemoteType.REMOTE_3) "Slider: $sliderValue" else activeCommand.displayName
                                            val cId = if (selectedRemote == RemoteType.REMOTE_3) 0x0001 else activeCommand.companyId
                                            val hex = when(selectedRemote) {
                                                RemoteType.REMOTE_1 -> "AA"
                                                RemoteType.REMOTE_2 -> "BB"
                                                RemoteType.REMOTE_3 -> "CC ${sliderValue.toString(16).padStart(2,'0').uppercase()}"
                                            }
                                            addToHistory(advertisingHistory, label, selectedRemote, cId, hex)
                                        },
                                        onFailure = { isAdvActive = false; advertisingTime = 0 }
                                    )
                                    currentCallback = cb
                                    when {
                                        isDiscoEnabled -> startAdvertising(bluetoothAdvertiser, cb, RemoteType.REMOTE_1, activeCommand)
                                        selectedRemote == RemoteType.REMOTE_3 -> sendSliderCommand(bluetoothAdvertiser, cb, sliderValue)
                                        else -> startAdvertising(bluetoothAdvertiser, cb, selectedRemote, activeCommand)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .scale(if (isAdvActive) hapticPulse else 1f),
                            isActive = true
                        )
                    }
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
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassInsetBox(
                modifier = Modifier.size(48.dp),
                cornerShape = CircleShape
            ) {
                if (isAdvertising) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAdvertising) commandName else "Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )

                Text(
                    text = if (isAdvertising) "Transmitting" else "Inactive",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAdvertising) accentColor else TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isAdvertising) time else "--:--",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (isAdvertising) "0x${companyId.toString(16).padStart(4, '0').uppercase()}" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
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
            .padding(4.dp)
            .clickable { onSelect(command) },
        contentAlignment = Alignment.Center
    ) {
        GlassInsetBox(
            modifier = Modifier.fillMaxSize(),
            cornerShape = RoundedCornerShape(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(command.color)
                        .then(
                            if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                        )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cmdDisplayName(command.displayName),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
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
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(command) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassInsetBox(
                modifier = Modifier.size(48.dp),
                cornerShape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(command.color)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = command.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = { onSelect(command) },
                colors = RadioButtonDefaults.colors(selectedColor = MintGreenAccent)
            )
        }
    }
}

@Composable
fun DiscoControlCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    speed: Long,
    onSpeedChange: (Long) -> Unit,
    isRandom: Boolean,
    onRandomToggle: (Boolean) -> Unit,
    isStrobe: Boolean,
    onStrobeToggle: (Boolean) -> Unit
) {
    val discoColor1 = Color(0xFFFF00FF)
    val discoColor2 = Color(0xFF00FFFF)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassInsetBox(modifier = Modifier.size(52.dp), cornerShape = CircleShape) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .padding(2.dp)
                    ) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).background(if (isEnabled) Color.Black else Color.White), contentAlignment = Alignment.Center) {
                            Icon(if (isEnabled) Icons.Default.FlashOn else Icons.Default.MusicNote, null, tint = if (isEnabled) Color.White else Color.Black, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Disco Vibes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (isEnabled) "INSANE VIBES ACTIVE" else "Party light show", style = MaterialTheme.typography.labelSmall, color = if (isEnabled) discoColor2 else TextSecondary)
                }
                Switch(checked = isEnabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = discoColor2))
            }

            if (isEnabled) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Strobe Flashes", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isStrobe, onCheckedChange = onStrobeToggle, colors = SwitchDefaults.colors(checkedThumbColor = discoColor1))
                }
                Spacer(Modifier.height(16.dp))
                Text("Vibe Speed", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Slider(value = (1100L - speed).toFloat(), onValueChange = { onSpeedChange(1100L - it.toLong()) }, valueRange = 100f..1050f, colors = SliderDefaults.colors(thumbColor = discoColor1, activeTrackColor = discoColor1))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shuffle Mode", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Checkbox(checked = isRandom, onCheckedChange = onRandomToggle, colors = CheckboxDefaults.colors(checkedColor = discoColor2))
                }
            }
        }
    }
}

@Composable
fun CycleIntervalControl(interval: Int, onIntervalChange: (Int) -> Unit, accentColor: Color) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Automatic Transition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                GlassInsetBox(cornerShape = RoundedCornerShape(12.dp)) {
                    Text(text = "${interval}s Delay", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Slider(value = interval.toFloat(), onValueChange = { onIntervalChange(it.toInt()) }, valueRange = 1f..5f, steps = 3, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor, inactiveTrackColor = accentColor.copy(alpha = 0.2f)))
            Text(text = "Transition to the next color state every $interval seconds.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun IntensitySlider(value: Int, onValueChange: (Int) -> Unit, accentColor: Color) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Luminance Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                GlassInsetBox(cornerShape = RoundedCornerShape(12.dp)) {
                    Text(text = "Level $value", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = accentColor)
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))))
            Slider(value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = 0f..255f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent), modifier = Modifier.offset(y = (-17).dp))
            Text(text = "Command: 0xCC [${value.toString(16).padStart(2,'0').uppercase()}]", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextSecondary.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun EnhancedHistorySection(history: List<AdvertisingHistory>) {
    Column {
        Text(text = "Telemetry Command Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                history.take(5).forEachIndexed { index, entry ->
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassInsetBox(modifier = Modifier.size(40.dp), cornerShape = CircleShape) {
                            Box(Modifier.size(16.dp).clip(CircleShape).background(entry.remoteType.color))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "ID: 0x${entry.companyId.toString(16).uppercase()} • Raw: 0x${entry.dataHex}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    if (index < history.take(5).size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = TextSecondary.copy(alpha = 0.1f))
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

private fun startAdvertising(advertiser: BluetoothLeAdvertiser?, callback: AdvertiseCallback, remote: RemoteType, command: LEDCommand) {
    try {
        if (advertiser == null) return
        val companyId = command.companyId
        val data      = if (remote == RemoteType.REMOTE_1) byteArrayOf(SECRET_KEY_SOLID) else byteArrayOf(SECRET_KEY_ANIMATION)
        advertiser.startAdvertising(buildSettings(), buildAdvData(companyId, data), callback)
    } catch (e: SecurityException) { e.printStackTrace() }
    catch (e: Exception)          { e.printStackTrace() }
}

private fun sendSliderCommand(advertiser: BluetoothLeAdvertiser?, callback: AdvertiseCallback, sliderValue: Int) {
    try {
        if (advertiser == null) return
        val companyId = 0x0001
        val data      = byteArrayOf(SECRET_KEY_SLIDER, sliderValue.toByte())
        advertiser.startAdvertising(buildSettings(), buildAdvData(companyId, data), callback)
    } catch (e: SecurityException) { e.printStackTrace() }
    catch (e: Exception)          { e.printStackTrace() }
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

private fun createAdvertiseCallback(onSuccess: () -> Unit, onFailure: () -> Unit): AdvertiseCallback = object : AdvertiseCallback() {
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

private fun addToHistory(history: MutableList<AdvertisingHistory>, label: String, remoteType: RemoteType, companyId: Int, dataHex: String) {
    history.add(0, AdvertisingHistory(label, System.currentTimeMillis(), remoteType, companyId, dataHex))
    if (history.size > 10) history.removeAt(history.lastIndex)
}
