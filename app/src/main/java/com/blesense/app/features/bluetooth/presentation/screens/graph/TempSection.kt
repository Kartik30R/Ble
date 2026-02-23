package com.blesense.app.features.bluetooth.presentation.screens.graph
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.presentation.widget.graph.SensorGraphCard


@Composable
fun TempSection(data: SensorData.SHT40Data) {

    val temp = data.temperature?.toFloatOrNull()
    val hum = data.humidity?.toFloatOrNull()

    val tempH = remember { mutableStateListOf<Float>() }
    val humH = remember { mutableStateListOf<Float>() }

    LaunchedEffect(temp) {
        temp?.let {
            if (tempH.size >= 50) tempH.removeAt(0)
            tempH.add(it)
        }
    }

    LaunchedEffect(hum) {
        hum?.let {
            if (humH.size >= 50) humH.removeAt(0)
            humH.add(it)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        SensorGraphCard(
            title = "Temperature (°C)",
            cur = temp,
            hist = tempH,
            lineCol = MaterialTheme.colorScheme.error,
            cardBg = MaterialTheme.colorScheme.surface,
            txtCol = MaterialTheme.colorScheme.onSurface,
            txt2Col = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            curLabel = "Current",
            naLabel = "N/A",
            dark = false
        )

        SensorGraphCard(
            title = "Humidity (%)",
            cur = hum,
            hist = humH,
            lineCol = MaterialTheme.colorScheme.primary,
            cardBg = MaterialTheme.colorScheme.surface,
            txtCol = MaterialTheme.colorScheme.onSurface,
            txt2Col = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            curLabel = "Current",
            naLabel = "N/A",
            dark = false
        )
    }
}