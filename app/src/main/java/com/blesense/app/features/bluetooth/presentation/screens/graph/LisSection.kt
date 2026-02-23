package com.blesense.app.features.bluetooth.presentation.screens.graph
import androidx.compose.foundation.layout.*
import androidx.compose.material.Colors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blesense.app.features.bluetooth.domain.model.AccPoint
import com.blesense.app.features.bluetooth.domain.model.SensorData
import com.blesense.app.features.bluetooth.presentation.widget.graph.Accelerometer3DVisualization
import com.blesense.app.features.bluetooth.presentation.widget.graph.AccelerometerGraphCard

@Composable
fun LisSection(data: SensorData.LIS2DHData) {

    val accX = data.x?.toFloatOrNull()
    val accY = data.y?.toFloatOrNull()
    val accZ = data.z?.toFloatOrNull()

    val accXH = remember { mutableStateListOf<AccPoint>() }
    val accYH = remember { mutableStateListOf<AccPoint>() }
    val accZH = remember { mutableStateListOf<AccPoint>() }

    LaunchedEffect(accX) {
        accX?.let {
            if (accXH.size >= 50) accXH.removeAt(0)
            accXH.add(AccPoint(System.nanoTime(), it))
        }
    }

    LaunchedEffect(accY) {
        accY?.let {
            if (accYH.size >= 50) accYH.removeAt(0)
            accYH.add(AccPoint(System.nanoTime(), it))
        }
    }

    LaunchedEffect(accZ) {
        accZ?.let {
            if (accZH.size >= 50) accZH.removeAt(0)
            accZH.add(AccPoint(System.nanoTime(), it))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        AccelerometerGraphCard(
            title = "X Axis (g)",
            cur = accX,
            hist = accXH,
            lineCol = MaterialTheme.colorScheme.primary,
            cardBg = MaterialTheme.colorScheme.surface,
            txtCol = MaterialTheme.colorScheme.onSurface,
            txt2Col = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            curLabel = "Current",
            naLabel = "N/A",
            dark = false
        )

        AccelerometerGraphCard(
            title = "Y Axis (g)",
            cur = accY,
            hist = accYH,
            lineCol = Color.Red,
            cardBg = MaterialTheme.colorScheme.surface,
            txtCol = MaterialTheme.colorScheme.onSurface,
            txt2Col = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            curLabel = "Current",
            naLabel = "N/A",
            dark = false
        )

        AccelerometerGraphCard(
            title = "Z Axis (g)",
            cur = accZ,
            hist = accZH,
            lineCol = Color.Green,
            cardBg = MaterialTheme.colorScheme.surface,
            txtCol = MaterialTheme.colorScheme.onSurface,
            txt2Col = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            curLabel = "Current",
            naLabel = "N/A",
            dark = false
        )

        Accelerometer3DVisualization(accX, accY, accZ)
    }
}