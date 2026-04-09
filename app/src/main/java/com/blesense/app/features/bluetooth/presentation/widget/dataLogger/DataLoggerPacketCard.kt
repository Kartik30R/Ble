package com.blesense.app.features.bluetooth.presentation.widget.dataLogger
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blesense.app.coreui.constants.AppStrings
import com.blesense.app.features.bluetooth.domain.model.SensorData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



import com.blesense.app.coreui.components.GlassCard
import com.blesense.app.coreui.components.GlassInsetBox
import com.blesense.app.coreui.theme.MintGreenAccent
import com.blesense.app.coreui.theme.TextPrimary
import com.blesense.app.coreui.theme.TextSecondary
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DataLoggerPacketCard(
    packet: SensorData.DataLoggerData,
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {

    val accelPoints = remember(packet.payloadAccel) {
        val original = packet.payloadAccel

        val fixed = when {
            original.size >= 80 -> original.take(80)
            original.isNotEmpty() -> {
                val filled = original.toMutableList()
                val last = original.last()
                repeat(80 - original.size) { filled.add(last) }
                filled
            }
            else -> List(80) { Triple(0, 0, 0) }
        }

        fixed.map { triple ->
            val x = triple.first.toInt() and 0xFF
            val y = triple.second.toInt() and 0xFF
            val z = triple.third.toInt() and 0xFF

            if (x == 255 && y == 255 && z == 255) {
                Triple("--", "--", "--")
            } else {
                Triple(x.toString(), y.toString(), z.toString())
            }
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (isFirst) {
                Text(
                    text = "High-Fidelity Telemetry Packet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Packet ID: ${packet.currentPacketId}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintGreenAccent
                    )
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(packet.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                val byteCount = packet.rawData.split(" ").size
                GlassInsetBox(
                    cornerShape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$byteCount Bytes",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Accelerometer Samples (80 Points)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Recessed Data Grid
            GlassInsetBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                cornerShape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(accelPoints) { index, triple ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
                                text = "X: ${triple.first}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (triple.first == "--") TextSecondary else Color(0xFFFF5252),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Y-Axis (Mint)
                            Text(
                                text = "Y: ${triple.second}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (triple.second == "--") TextSecondary else MintGreenAccent,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            // Z-Axis (Cyan)
                            Text(
                                text = "Z: ${triple.third}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (triple.third == "--") TextSecondary else Color(0xFF00E5FF),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Raw Hexadecimal Stream",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassInsetBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                cornerShape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    packet.rawData.chunked(60).forEach { chunk ->
                        Text(
                            text = chunk,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Light,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}
