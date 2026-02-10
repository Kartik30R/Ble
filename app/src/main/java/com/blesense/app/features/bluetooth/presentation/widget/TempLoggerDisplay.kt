package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.features.bluetooth.domain.model.SensorData
import presentation.viewmodel.BluetoothScanViewModel

@Composable
fun TempLoggerDisplay(
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceId: String,
    deviceName: String
) {
    // Collect history from the specialized flow in your ViewModel
    val history by viewModel.observeTempLoggerHistory(deviceAddress).collectAsState(initial = emptyList())

    // Filter for large packets (log data packets are typically 224 bytes vs 32 byte advertising packets)
    val largePackets = remember(history) {
        history.filter { packet ->
            val byteCount = packet.rawData.split(" ").count { it.isNotBlank() }
            byteCount >= 224
        }
    }

    val latestLargePacket = largePackets.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Log Data (${largePackets.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (latestLargePacket != null) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "ID: $deviceId | ${deviceAddress.takeLast(8)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (largePackets.isEmpty()) {
            EmptyLargePacketState(deviceAddress)
            return
        }

        // Statistics Summary Card
        TempLoggerStatsCard(largePackets, latestLargePacket)

        Spacer(modifier = Modifier.height(12.dp))

        // History List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(largePackets.reversed()) { packet ->
                TempLoggerPacketCard(
                    packet = packet,
                    isLatest = packet == latestLargePacket,
                    deviceName = deviceName
                )
            }
        }
    }
}

@Composable
private fun TempLoggerPacketCard(
    packet: SensorData.TempLoggerData,
    isLatest: Boolean,
    deviceName: String
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLatest) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Packet Data",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.primary
                )
                if (isLatest) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "(Latest)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReadingItem("Temp", "${packet.temperature}°C")
                ReadingItem("Hum", "${packet.humidity}%")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Raw Data Expandable Area
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (expanded) "▼ Hide Raw Hex" else "▶ Show Raw Hex",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (expanded) {
                        Text(
                            text = packet.rawData,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyLargePacketState(address: String) {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Text(
            "Waiting for 224-byte log packets...\n(Device: ${address.takeLast(4)})",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}