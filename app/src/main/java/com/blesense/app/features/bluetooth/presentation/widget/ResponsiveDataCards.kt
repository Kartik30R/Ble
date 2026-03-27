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
import com.blesense.app.coreui.constants.AppStrings

@Composable
fun ResponsiveDataCards(
    data: List<Pair<String, String>>
) {
    // 1. Separate special data types using centralized AppStrings
    val ammoniaData = data.find { it.first == AppStrings.AMMONIA }
    val rawData = data.find { it.first == AppStrings.RAW_DATA }
    val otherData = data.filterNot {
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
                    DataCard(label = label, value = value)
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
    value: String
) {
    val numericValue = value.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 0f

    // 2. Dynamic Status Colors (Green/Yellow/Red logic)
    val statusColor = when (label) {
        AppStrings.TEMPERATURE -> when {
            numericValue <= 15f -> Color(0xFF2196F3) // Cold
            numericValue <= 30f -> Color(0xFF4CAF50) // Safe
            else -> Color(0xFFF44336)                // High
        }
        AppStrings.HUMIDITY -> when {
            numericValue <= 40f -> Color(0xFF03A9F4) // Dry
            numericValue <= 70f -> Color(0xFF4CAF50) // Ideal
            else -> Color(0xFFE91E63)                // Humid
        }
        AppStrings.PM1, AppStrings.PM2_5, AppStrings.PM4 -> when {
            numericValue <= 12f -> Color(0xFF4CAF50) // Good
            numericValue <= 35f -> Color(0xFFFFC107) // Moderate
            else -> Color(0xFFF44336)                // High
        }
        AppStrings.PM10 -> when {
            numericValue <= 54f -> Color(0xFF4CAF50) // Good
            numericValue <= 154f -> Color(0xFFFFC107) // Moderate
            else -> Color(0xFFF44336)                // High
        }
        AppStrings.CO2 -> when {
            numericValue <= 800f -> Color(0xFF4CAF50) // Good
            numericValue <= 1200f -> Color(0xFFFFC107) // Fair
            else -> Color(0xFFF44336)                // Poor
        }
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    ElevatedCard(
        modifier = Modifier
            .size(width = 145.dp, height = 115.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusColor
        )
    ) {
        // Apply a subtle gradient overlay for the "Glassmorphism" look from your old UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (statusColor == MaterialTheme.colorScheme.primaryContainer) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (statusColor == MaterialTheme.colorScheme.primaryContainer) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}