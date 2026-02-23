package com.blesense.app.features.bluetooth.presentation.screens.graph

import androidx.compose.foundation.layout.*
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

    // X
    LaunchedEffect(accX) {
        accX?.let {
            accXH.add(AccPoint(System.nanoTime(), it))
        }
    }

    // Y
    LaunchedEffect(accY) {
        accY?.let {
            accYH.add(AccPoint(System.nanoTime(), it))
        }
    }

    // Z
    LaunchedEffect(accZ) {
        accZ?.let {
            accZH.add(AccPoint(System.nanoTime(), it))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        AccelerometerGraphCard(
            title = "X Axis (g)",
            cur = accX,
            hist = accXH,
            lineCol = MaterialTheme.colorScheme.primary,

            curLabel = "Current",
            naLabel = "N/A",
         )

        AccelerometerGraphCard(
            title = "Y Axis (g)",
            cur = accY,
            hist = accYH,
            lineCol = Color.Red,

            curLabel = "Current",
            naLabel = "N/A",
         )

        AccelerometerGraphCard(
            title = "Z Axis (g)",
            cur = accZ,
            hist = accZH,
            lineCol = Color.Green,

            curLabel = "Current",
            naLabel = "N/A",
         )

        // Keep 3D visualizer unchanged
        Accelerometer3DVisualization(accX, accY, accZ)
    }
}