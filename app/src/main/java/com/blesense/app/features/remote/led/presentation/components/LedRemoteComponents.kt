package com.blesense.app.features.remote.led.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.features.remote.led.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CompactStatusCard(
    isAdvertising: Boolean,
    time: Int,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdvertising) accentColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isAdvertising) formatTime(time) else "--:--",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAdvertising) "0x${companyId.toString(16).padStart(4, '0').uppercase()}" else "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
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
                    .shadow(if (isSelected) 8.dp else 0.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (command.color.toByte() > 0x80.toByte()) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (command.displayName == "Indian Flag") "Flag" else command.displayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Color.toByte(): Byte {
    return ((red + green + blue) / 3f * 255f).toInt().toByte()
}

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
fun CyclePlaylistHeader(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    interval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val accentColor = Color(0xFF673AB7)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isActive) Icons.Default.Sync else Icons.Default.SyncDisabled,
                    contentDescription = null,
                    tint = if (isActive) accentColor else Color.Gray
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cycle Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (isActive) "Looping selected colors" else "Single color mode", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                )
            }

            if (isActive) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Interval", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(Modifier.weight(1f))
                    Text("${interval}s", style = MaterialTheme.typography.labelLarge, color = accentColor, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = interval.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
        }
    }
}
@Composable
fun DiscoControlCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val discoColor2 = Color(0xFF00FFFF)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isEnabled) Color(0xFF121212) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
                        )
                        .padding(2.dp)
                ) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).background(if (isEnabled) Color.Black else Color.White), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isEnabled) Icons.Default.FlashOn else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isEnabled) Color.White else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Disco Vibes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isEnabled) Color.White else Color.Black)
                    Text(if (isEnabled) "INSANE VIBES ACTIVE" else "Party light show", style = MaterialTheme.typography.labelSmall, color = if (isEnabled) discoColor2 else Color.Gray)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = discoColor2)
                )
            }
        }
    }
}

@Composable
fun CustomCycleControl(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    primaryColor: LEDCommand,
    onPrimarySelect: (LEDCommand) -> Unit,
    secondaryColor: LEDCommand,
    onSecondarySelect: (LEDCommand) -> Unit,
    interval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val accentColor = Color(0xFFFF9800)
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isEnabled) Color(0xFF121212) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Custom Cycle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isEnabled) Color.White else Color.Black)
                    Text(if (isEnabled) "Alternating colors..." else "Set your own loop", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                )
            }

            if (isEnabled) {
                Spacer(Modifier.height(24.dp))
                
                Text("Select Colors", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallColorSwatch(
                        modifier = Modifier.weight(1f),
                        label = "First",
                        command = primaryColor,
                        onClick = { onPrimarySelect(it) }
                    )
                    SmallColorSwatch(
                        modifier = Modifier.weight(1f),
                        label = "Second",
                        command = secondaryColor,
                        onClick = { onSecondarySelect(it) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Text("${interval}s", style = MaterialTheme.typography.labelLarge, color = accentColor, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = interval.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
        }
    }
}

@Composable
fun SmallColorSwatch(
    modifier: Modifier = Modifier,
    label: String,
    command: LEDCommand,
    onClick: (LEDCommand) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedCard(
        onClick = { showDialog = true },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(command.color))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(command.displayName, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose Color") },
            text = {
                val commands = LEDCommand.values().filter { it.mode == "Solid" && it != LEDCommand.SOLID_OFF }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commands) { cmd ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cmd.color)
                                .clickable { onClick(cmd); showDialog = false }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun VisualizerControl(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    level: Float,
    hue: Float
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive) Color(0xFF121212) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
                        )
                ) {
                    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isActive) Icons.Default.Equalizer else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isActive) Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Audio Visualizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isActive) Color.White else Color.Black)
                    Text(if (isActive) "Listening to environment..." else "Capture beat vibes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan)
                )
            }

            if (isActive) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(15) { i ->
                        val h = (level * 40f * (0.5f + (i % 5) * 0.1f)).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(h)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + i * 10) % 360, 1f, 1f))))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodLibrary(
    activeMood: Mood?,
    onMoodSelect: (Mood) -> Unit
) {
    Text(
        "Mood Presets",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(MOOD_LIBRARY) { mood ->
            MoodCard(
                mood = mood,
                isSelected = activeMood == mood,
                onSelect = { onMoodSelect(mood) }
            )
        }
    }
}

@Composable
fun MoodCard(
    mood: Mood,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) BorderStroke(2.dp, mood.gradient.first()) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(mood.gradient, start = Offset(0f, 0f), end = Offset(400f, 400f)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(mood.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
                    Text(mood.description, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                }
            }
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = accentColor)
                Spacer(Modifier.width(12.dp))
                Text("Intensity Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accentColor)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.toInt()) },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun EnhancedHistorySection(history: List<AdvertisingHistory>) {
    Column {
        Text("Recent Commands", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column {
                history.take(4).forEachIndexed { index, entry ->
                    ListItem(
                        headlineContent = { Text(entry.label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        supportingContent = { Text("Comp: 0x${entry.companyId.toString(16).padStart(4,'0').uppercase()} • Data: 0x${entry.dataHex}", style = MaterialTheme.typography.bodySmall) },
                        trailingContent = { Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall) },
                        leadingContent = { Box(Modifier.size(10.dp).clip(CircleShape).background(entry.remoteType.color)) }
                    )
                    if (index < history.take(4).size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
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
        title = { Text("Device Access Needed") },
        text = { Text("BLE Sense requires Bluetooth advertising permissions to communicate with your LED devices.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Allow Access") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } }
    )
}

@Composable
fun PermissionsWarningCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GppMaybe, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.size(12.dp))
            Text("Advertising permissions required", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
