package com.blesense.app.features.bluetooth.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blesense.app.coreui.components.GlassCard
import com.blesense.app.coreui.components.GlassInsetBox
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.constants.AppStrings

@Composable
fun ResponsiveDataCards(
    displayData: List<Pair<String, String>>,
    isAlarmActive: Boolean = false,
    blinkAlpha: Float = 0f
) {
    // 1. Separate special data types using centralized AppStrings
    val ammoniaData = displayData.find { it.first == AppStrings.AMMONIA }
    val rawData = displayData.find { it.first == AppStrings.RAW_DATA }
    val otherData = displayData.filterNot {
        it.first == AppStrings.AMMONIA || it.first == AppStrings.RAW_DATA || it.first == AppStrings.NODE_ID_LABEL
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Ammonia Special Display ---
        ammoniaData?.let { (label, value) ->
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Retain your existing custom animation widget
                AmmoniaRingAnimation(
                    ammoniaValue = value.replace(" ppm", "").toFloatOrNull() ?: 0f
                )
            }
        }

        // --- Raw Data Display ---
        rawData?.let { (_, value) ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.RAW_DATA,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Grid Layout for Regular Sensors ---
        otherData.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                rowItems.forEach { (label, value) ->
                    DataCard(
                        label = label, 
                        value = value,
                        isAlarming = isAlarmActive,
                        blinkAlpha = blinkAlpha
                    )
                }
                // Maintain alignment if row is incomplete
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.width(141.dp))
                }
            }
        }
    }
}

@Composable
fun DataCard(
    label: String,
    value: String,
    isAlarming: Boolean = false,
    blinkAlpha: Float = 0f
) {
    val alarmColor = Color(0xFFFF4848).copy(alpha = blinkAlpha)
    
    GlassCard(
        modifier = Modifier
            .size(width = 160.dp, height = 130.dp)
            .padding(4.dp)
            .then(
                if (isAlarming) Modifier.background(
                    Brush.radialGradient(listOf(alarmColor, Color.Transparent))
                ) else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Recessed (Inset) Value Container
            GlassInsetBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                cornerShape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAlarming) Color(0xFFFF5252) else MintGreenAccent,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}