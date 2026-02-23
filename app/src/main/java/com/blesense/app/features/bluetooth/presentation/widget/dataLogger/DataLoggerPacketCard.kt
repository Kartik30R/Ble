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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            if (isFirst) {
                Text(
                    text = "DataLogger - Large Data Packets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Packet ID: ${packet.currentPacketId}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Timestamp: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date(packet.timestamp))
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accelerometer (80 Points)",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {

                accelPoints.forEachIndexed { index, triple ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#${(index + 1).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${AppStrings.X_AXIS}: ${triple.first}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${AppStrings.Y_AXIS}: ${triple.second}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${AppStrings.Z_AXIS}: ${triple.third}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val byteCount = packet.rawData.split(" ").size

            Text(
                text = "${AppStrings.RAW_DATA} ($byteCount bytes)",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {

                packet.rawData.chunked(64).forEach { chunk ->

                    Text(
                        text = chunk,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total payload: $byteCount bytes",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
