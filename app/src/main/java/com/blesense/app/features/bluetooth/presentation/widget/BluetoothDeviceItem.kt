package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.* // M3 version
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blesense.app.features.bluetooth.domain.model.BleDevice
import com.blesense.app.features.bluetooth.domain.model.SensorData
import java.text.SimpleDateFormat
import java.util.*
import com.blesense.app.R

@Composable
fun BluetoothDeviceItem(
    device: BleDevice, // Using Domain Model
    navController: NavHostController,
    selectedSensor: String,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
             .clickable {
                 val safeDeviceId = if (device.deviceId.isNullOrBlank()) "unknown" else device.deviceId

                 val safeName = device.name.replace("/", "-").ifBlank { "Unknown" }

                navController.navigate(
                    "advertising/$safeName/${device.address}/$selectedSensor/$safeDeviceId"
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bluetooth icon container using M3 ColorScheme
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Device information column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name, 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Address: ${device.address} | RSSI: ${device.rssi} dBm", 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Display sensor-specific data preview
            device.sensorData?.let { data ->
                val displayText = getPreviewText(selectedSensor, data)

                // Special preview for DataLogger
                if (selectedSensor == "DataLogger" && data is SensorData.DataLoggerData) {
                    DataLoggerPreview(
                        rawData = displayText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } ?: Text(
                "Waiting for data...", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Logic helper for preview strings
private fun getPreviewText(selected: String, data: SensorData): String {
    return when (data) {
        is SensorData.SHT40Data -> "Temp: ${data.temperature}°C, Hum: ${data.humidity}%"
        is SensorData.TempLoggerData -> "Temp: ${data.temperature}°C, Hum: ${data.humidity}%"
        is SensorData.LIS2DHData -> "X: ${data.x}, Y: ${data.y}, Z: ${data.z}"
        is SensorData.LuxSensorData -> "Brightness: ${data.lux} Lux"
        is SensorData.SoilSensorData -> "N:${data.nitrogen} P:${data.phosphorus} K:${data.potassium}"
        is SensorData.SDTData -> "Speed: ${data.speed}m/s, Dist: ${data.distance}m"
        is SensorData.AmmoniaSensorData -> "NH3: ${data.ammonia}"
        is SensorData.DataLoggerData -> {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))
            "ID: ${data.currentPacketId} | Points: ${data.payloadAccel.size} | $time"
        }
        else -> "Unknown Sensor Type"
    }
}