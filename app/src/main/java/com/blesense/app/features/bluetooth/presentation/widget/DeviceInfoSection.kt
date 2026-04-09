package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.coreui.constants.AppStrings

import com.blesense.app.coreui.components.GlassCard
import com.blesense.app.coreui.components.GlassInsetBox
import com.blesense.app.coreui.theme.TextPrimary
import com.blesense.app.coreui.theme.TextSecondary
import com.blesense.app.features.bluetooth.domain.model.SensorData

@Composable
fun DeviceInfoSection(
    deviceName: String,
    deviceAddress: String,
    sensorData: SensorData?
) {
    val nodeID = when (sensorData) {
        is SensorData.SHT40Data -> sensorData.deviceId
        is SensorData.SDTData -> sensorData.deviceId
        is SensorData.LIS2DHData -> sensorData.deviceId
        is SensorData.SoilSensorData -> sensorData.deviceId
        is SensorData.LuxSensorData -> sensorData.deviceId
        is SensorData.AmmoniaSensorData -> sensorData.deviceId
        is SensorData.TempLoggerData -> sensorData.deviceId
        is SensorData.DataLoggerData -> sensorData.deviceId
        is SensorData.Sen6xData -> sensorData.deviceId
        else -> "--"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recessed (Inset) Icon Pillar
            GlassInsetBox(
                modifier = Modifier.size(56.dp),
                cornerShape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.blesense.app.R.drawable.bg_remove_ble),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = deviceAddress,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Node ID: $nodeID",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}