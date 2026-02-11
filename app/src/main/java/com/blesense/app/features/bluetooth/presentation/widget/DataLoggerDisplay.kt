package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.viewmodel.BluetoothScanViewModel

@Composable
fun DataLoggerDisplay(
    viewModel: BluetoothScanViewModel
) {
    val packetHistory by viewModel.dataLoggerPacketHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(
            text = "Packets History (${packetHistory.size})",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Empty state
        if (packetHistory.isEmpty()) {
            Text(
                text = "No packets received yet",
                color = Color.Gray
            )
            return
        }

        // Use ONLY the latest packet (advertisement-safe)
        val packet = packetHistory.last()

        Text(
            text = "Packet ID: ${packet.lastPacketId}",
            color = Color.Cyan,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val accel = packet.payloadAccel

        if (accel.isEmpty()) {
            Text(
                text = "No accelerometer data",
                color = Color.Gray
            )
            return
        }

        // Show only first 20 points (lightweight)
        accel.take(20).forEachIndexed { index, triple ->

            val x = triple.first.toInt() and 0xFF
            val y = triple.second.toInt() and 0xFF
            val z = triple.third.toInt() and 0xFF

            val isInvalid = (x == 255 && y == 255 && z == 255)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "#${index + 1}",
                    color = Color.Gray
                )

                Text(
                    text = if (isInvalid) "X: --" else "X: $x",
                    color = if (isInvalid) Color.Gray else Color.Red
                )

                Text(
                    text = if (isInvalid) "Y: --" else "Y: $y",
                    color = if (isInvalid) Color.Gray else Color.Green
                )

                Text(
                    text = if (isInvalid) "Z: --" else "Z: $z",
                    color = if (isInvalid) Color.Gray else Color.Cyan
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
