package com.blesense.app.features.bluetooth.presentation.screens.graph
 import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
 import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import presentation.viewmodel.BluetoothScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    navController: NavController,
    deviceAddress: String?,
    viewModel: BluetoothScanViewModel
) {

    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(

                title = {
                    Text(
                        text = "Graphs",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        if (deviceAddress != null) {

            DeviceSection(
                deviceAddress = deviceAddress,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        }
    }
}