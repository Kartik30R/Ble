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
import com.blesense.app.coreui.components.GlassCard
import com.blesense.app.coreui.components.GlassInsetBox
import com.blesense.app.coreui.theme.*

@Composable
fun BluetoothDeviceItem(
    device: BleDevice,
    navController: NavHostController,
    selectedSensor: String,
    isDarkMode: Boolean
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = {
            val safeDeviceId = if (device.deviceId.isNullOrBlank()) "unknown" else device.deviceId
            val safeName = device.name.replace("/", "-").ifBlank { "Unknown" }
            navController.navigate(
                "advertising/$safeName/${device.address}/$selectedSensor/$safeDeviceId"
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recessed (Inset) Icon Container
            GlassInsetBox(
                modifier = Modifier.size(52.dp),
                cornerShape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MintGreenAccent
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Address: ${device.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_turbo), // Using as signal icon
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MintGreenAccent.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintGreenAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sensor preview
                device.sensorData?.let { data ->
                    val displayText = getPreviewText(selectedSensor, data)
                    if (selectedSensor == "DataLogger" && data is SensorData.DataLoggerData) {
                        DataLoggerPreview(
                            rawData = displayText,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MintGreenAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } ?: Text(
                    "Waiting for data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled
                )
            }
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
        is SensorData.Sen6xData -> "PM2.5: ${data.pm25}, CO2: ${data.co2} ppm"
        is SensorData.DataLoggerData -> {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))
            "ID: ${data.currentPacketId} | Points: ${data.payloadAccel.size} | $time"
        }
        else -> "Unknown Sensor Type"
    }
}