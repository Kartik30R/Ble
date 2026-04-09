package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.viewmodel.BluetoothScanViewModel

import com.blesense.app.coreui.components.GlassInsetBox
import com.blesense.app.coreui.theme.MintGreenAccent
import com.blesense.app.coreui.theme.TextPrimary
import com.blesense.app.coreui.theme.TextSecondary
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment

@Composable
fun DataLoggerDisplay(
    viewModel: BluetoothScanViewModel
) {
    val packetHistory by viewModel.dataLoggerPacketHistory.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Recessed Count Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Live Packet Stream",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            GlassInsetBox(
                modifier = Modifier.padding(horizontal = 4.dp),
                cornerShape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${packetHistory.size} Pkts",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreenAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Empty state
        if (packetHistory.isEmpty()) {
            GlassInsetBox(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                cornerShape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Awaiting first data packet...",
                    color = TextSecondary.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return
        }

        // Tactical Data View
        val packet = packetHistory.last()
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tracking Packet ID: ${packet.lastPacketId}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            val accel = packet.payloadAccel
            if (accel.isEmpty()) {
                Text("No accelerometer data available", color = TextSecondary)
                return
            }

            // Recessed Data Grid
            GlassInsetBox(
                modifier = Modifier.fillMaxWidth(),
                cornerShape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show only first 20 points (lightweight)
                    accel.take(20).forEachIndexed { index, triple ->
                        val x = triple.first.toInt() and 0xFF
                        val y = triple.second.toInt() and 0xFF
                        val z = triple.third.toInt() and 0xFF
                        val isInvalid = (x == 255 && y == 255 && z == 255)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("#%02d", index + 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(32.dp)
                            )

                            // X-Axis (Soft Red)
                            Text(
                                text = if (isInvalid) "X: --" else "X: $x",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isInvalid) TextSecondary else Color(0xFFFF5252),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Y-Axis (Mint)
                            Text(
                                text = if (isInvalid) "Y: --" else "Y: $y",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isInvalid) TextSecondary else MintGreenAccent,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Z-Axis (Cyan)
                            Text(
                                text = if (isInvalid) "Z: --" else "Z: $z",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isInvalid) TextSecondary else Color(0xFF00E5FF),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
