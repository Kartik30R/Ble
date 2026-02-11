package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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

/* ----------------------------- MAIN SCREEN ----------------------------- */

@Composable
fun TempLoggerDisplay(
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    deviceId: String,
    deviceName: String
) {
    val history: List<SensorData.TempLoggerData> by viewModel
        .observeTempLoggerHistory(deviceAddress)
        .collectAsState<List<SensorData.TempLoggerData>, List<SensorData.TempLoggerData>>(
            initial = emptyList()
        )
    val largePackets = remember(history) {
        history.filter {
             it.rawData.split(" ").count { b -> b.isNotBlank() } >= 224
        }
    }
    val latestPacket = largePackets.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(
            text = "Log Data (${largePackets.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(largePackets.reversed()) { packet ->
                TempLoggerPacketCard(
                    packet = packet,
                    isLatest = packet == latestPacket,
                    deviceName = deviceName
                )
            }
        }
    }
}

/* ----------------------------- PACKET CARD ----------------------------- */

@Composable
private fun TempLoggerPacketCard(
    packet: SensorData.TempLoggerData,
    isLatest: Boolean,
    deviceName: String
) {
    var expanded by remember { mutableStateOf(false) }

    val byteGroups = remember(packet.rawData) {
        parseTempLoggerRawDataIntoByteGroups(packet.rawData)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLatest)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$deviceName",
                    fontWeight = FontWeight.Bold
                )
                if (isLatest) {
                    Spacer(Modifier.width(8.dp))
                    Text("(LATEST)", color = Color.Green, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ReadingItem("Temp", "${packet.temperature}°C")
                ReadingItem("Hum", "${packet.humidity}%")
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        text = if (expanded) "▼ Hide Raw + Parsed Data"
                        else "▶ Show Raw + Parsed Data",
                        fontSize = 12.sp
                    )

                    if (expanded) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = packet.rawData,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        byteGroups.forEachIndexed { index, group ->
                            TempLoggerByteGroupItem(
                                groupNumber = index + 1,
                                bytes = group,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------- BYTE GROUP ITEM ----------------------------- */

@Composable
fun TempLoggerByteGroupItem(
    groupNumber: Int,
    bytes: List<String>,
    modifier: Modifier = Modifier
) {
    val hasData = bytes.any { it != "00" && it != "--" }
    val isEmpty = bytes.all { it == "--" }

    val (temp, hum) = remember(bytes) { extractTempHumidityFromGroup(bytes) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Color.DarkGray)
    ) {
        Column(Modifier.padding(12.dp)) {

            Text(
                text = if (isEmpty) "Group $groupNumber (Empty)" else "Group $groupNumber",
                fontWeight = FontWeight.Bold
            )

            if (hasData && temp != "--") {
                Text("🌡️ $temp   💧 $hum", fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.height(120.dp)
            ) {
                itemsIndexed(bytes.take(32)) { index, byte ->
                    Text(
                        text = byte,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(4.dp),
                        color = when {
                            byte == "--" -> Color.Gray
                            byte == "00" -> Color.DarkGray
                            else -> Color(0xFF00FF88)
                        }
                    )
                }
            }
        }
    }
}

/* ----------------------------- PARSER ----------------------------- */

private fun parseTempLoggerRawDataIntoByteGroups(rawData: String?): List<List<String>> {
    if (rawData.isNullOrBlank()) return List(7) { List(32) { "--" } }

    val bytes = rawData.split(" ").filter { it.isNotBlank() }
    val result = mutableListOf<List<String>>()

    for (chunk in bytes.chunked(32)) {
        if (chunk.all { it.equals("FF", true) }) continue

        val padded = chunk.toMutableList()
        while (padded.size < 32) padded.add("00")
        result.add(padded.take(32))

        if (result.size == 7) break
    }

    while (result.size < 7) result.add(List(32) { "--" })

    return result
}

/* ----------------------------- TEMP / HUM EXTRACT ----------------------------- */

private fun extractTempHumidityFromGroup(bytes: List<String>): Pair<String, String> {
    return try {
        val t = bytes[0].toInt(16) + bytes[1].toInt(16) / 100.0
        val h = bytes[2].toInt(16) + bytes[3].toInt(16) / 100.0
        "${"%.2f".format(t)}°C" to "${"%.2f".format(h)}%"
    } catch (e: Exception) {
        "--" to "--"
    }
}

/* ----------------------------- SMALL HELPERS ----------------------------- */

@Composable
private fun ReadingItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyLargePacketState(address: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Waiting for 224-byte packets\n(${address.takeLast(4)})",
            fontStyle = FontStyle.Italic,
            color = Color.Gray
        )
    }
}
