package com.blesense.app.features.bluetooth.presentation.screens.graph

 import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import presentation.viewmodel.BluetoothScanViewModel
import com.blesense.app.features.bluetooth.domain.model.SensorData
import kotlinx.coroutines.flow.map

@Composable
fun DeviceSection(
    deviceAddress: String,
    viewModel: BluetoothScanViewModel,
    modifier: Modifier = Modifier
) {

    val device by viewModel.devices
        .map { list -> list.find { it.address == deviceAddress } }
        .collectAsState(initial = null)

    val sensorData = device?.sensorData

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        when (sensorData) {

            is SensorData.SHT40Data -> {
                item {
                    TempSection(sensorData)
                }
            }

            is SensorData.LIS2DHData -> {
                item {
                    LisSection(sensorData)
                }
            }

            else -> {
                item {
                    Text("Waiting for sensor data...")
                }
            }
        }
    }
}