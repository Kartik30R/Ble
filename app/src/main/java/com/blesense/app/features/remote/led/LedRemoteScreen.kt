package com.blesense.app.features.remote.led

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.blesense.app.core.di.BluetoothModule
import com.blesense.app.features.remote.led.domain.model.*
import com.blesense.app.features.remote.led.presentation.LedRemoteViewModel
import com.blesense.app.features.remote.led.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertisingScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    // Inject ViewModel using custom DI
    val viewModel: LedRemoteViewModel = viewModel(
        factory = BluetoothModule.ledRemoteViewModelFactory()
    )

    // Collect States
    val isAdvertising by viewModel.isAdvertising.collectAsStateWithLifecycle()
    val activeCommand by viewModel.activeCommand.collectAsStateWithLifecycle()
    val selectedRemote by viewModel.selectedRemote.collectAsStateWithLifecycle()
    val isDiscoEnabled by viewModel.isDiscoEnabled.collectAsStateWithLifecycle()
    val isVisualizerActive by viewModel.isVisualizerActive.collectAsStateWithLifecycle()
    val selectedMood by viewModel.selectedMood.collectAsStateWithLifecycle()
    val sliderValue by viewModel.sliderValue.collectAsStateWithLifecycle()
    val audioHue by viewModel.audioHue.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val history by viewModel.advertisingHistory.collectAsStateWithLifecycle()
    val advertisingTime by viewModel.advertisingTime.collectAsStateWithLifecycle()
    
    val secondaryCommand by viewModel.secondaryCommand.collectAsStateWithLifecycle()
    val customInterval by viewModel.customInterval.collectAsStateWithLifecycle()
    val isCustomCycleEnabled by viewModel.isCustomCycleEnabled.collectAsStateWithLifecycle()
    
    val isCycleModeActive by viewModel.isCycleModeActive.collectAsStateWithLifecycle()
    val selectedCommands by viewModel.selectedCommands.collectAsStateWithLifecycle()

    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-sync permissions status would go here if needed in VM
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LED REMOTE", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.startAdvertising() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdvertising) MaterialTheme.colorScheme.error else selectedRemote.color
                        )
                    ) {
                        Icon(if (isAdvertising) Icons.Default.Stop else Icons.Default.BluetoothAudio, null)
                        Spacer(Modifier.width(12.dp))
                        Text(if (isAdvertising) "STOP" else "TRANSMIT")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                CompactStatusCard(
                    isAdvertising = isAdvertising,
                    time = advertisingTime,
                    commandName = when {
                        isVisualizerActive -> "Audio Match"
                        isCustomCycleEnabled -> "Custom Loop"
                        isCycleModeActive -> "Playlist (${selectedCommands.size})"
                        selectedMood != null -> "Mood: ${selectedMood?.name}"
                        isDiscoEnabled -> "RAVE DISCO"
                        else -> activeCommand.displayName
                    },
                    accentColor = selectedRemote.color,
                    companyId = activeCommand.companyId,
                    dataHex = "AA ${activeCommand.id.toString(16).padStart(2,'0').uppercase()}"
                )
            }

            item {
                SelectionSection(
                    selectedRemote = selectedRemote,
                    onRemoteSelect = { remote -> viewModel.onRemoteTypeChange(remote) }
                )
            }

            // Dynamic Content based on Tab
            when (selectedRemote) {
                RemoteType.REMOTE_1 -> item {
                    Column {
                        CyclePlaylistHeader(
                            isActive = isCycleModeActive,
                            onToggle = { active -> viewModel.toggleCycleMode(active) },
                            interval = customInterval,
                            onIntervalChange = { interval -> viewModel.onCustomIntervalChange(interval) }
                        )
                        Spacer(Modifier.height(24.dp))
                        DiscoControlCard(
                            isEnabled = isDiscoEnabled,
                            onToggle = { enabled -> viewModel.toggleDisco(enabled) }
                        )
                        Spacer(Modifier.height(24.dp))
                        Text("Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        PresetGrid(
                            selectedCommands = selectedCommands,
                            onCommandSelect = { viewModel.onCommandChange(it) }
                        )
                    }
                }
                RemoteType.REMOTE_2 -> items(LEDCommand.values().filter { it.mode == "Animation" }) { anim ->
                    AnimationListItem(
                        command = anim,
                        isSelected = activeCommand == anim,
                        onSelect = { cmd -> viewModel.onCommandChange(cmd) }
                    )
                }
                RemoteType.REMOTE_3 -> item {
                    IntensitySlider(
                        value = sliderValue,
                        onValueChange = { value -> viewModel.onSliderChange(value) },
                        accentColor = activeCommand.color
                    )
                }
                RemoteType.REMOTE_4 -> item {
                    Column {
                        VisualizerControl(
                            isActive = isVisualizerActive,
                            onToggle = { enabled -> viewModel.toggleVisualizer(enabled, context) },
                            level = audioLevel,
                            hue = audioHue
                        )
                        Spacer(Modifier.height(24.dp))
                        CustomCycleControl(
                            isEnabled = isCustomCycleEnabled,
                            onToggle = { enabled -> viewModel.toggleCustomCycle(enabled) },
                            primaryColor = activeCommand,
                            onPrimarySelect = { cmd -> viewModel.onCommandChange(cmd, false) },
                            secondaryColor = secondaryCommand,
                            onSecondarySelect = { cmd -> viewModel.onCommandChange(cmd, true) },
                            interval = customInterval,
                            onIntervalChange = { interval -> viewModel.onCustomIntervalChange(interval) }
                        )
                        Spacer(Modifier.height(24.dp))
                        MoodLibrary(
                            activeMood = selectedMood,
                            onMoodSelect = { mood -> viewModel.selectMood(mood) }
                        )
                    }
                }
            }

            item { EnhancedHistorySection(history = history) }
            item { Spacer(Modifier.height(48.dp)) }
        }

        if (showPermissionDialog) {
            PermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onConfirm = { showPermissionDialog = false /* request logic in VM */ }
            )
        }
    }
}

@Composable
fun PresetGrid(
    selectedCommands: List<LEDCommand>,
    onCommandSelect: (LEDCommand) -> Unit
) {
    val commands = LEDCommand.values().filter { it.mode == "Solid" }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        commands.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEach { cmd ->
                    ColorSwatch(
                        modifier = Modifier.weight(1f),
                        command = cmd,
                        isSelected = selectedCommands.contains(cmd),
                        onSelect = onCommandSelect
                    )
                }
                // Fill space if row is not full
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
