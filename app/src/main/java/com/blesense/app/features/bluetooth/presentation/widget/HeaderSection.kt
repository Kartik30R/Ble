package com.blesense.app.Presentation.widgets

 import android.net.Uri
 import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.blesense.app.R
import com.blesense.app.coreui.constants.AppStrings
import presentation.viewmodel.BluetoothScanViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    navController: NavController,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = AppStrings.ADVERTISING_DATA_TITLE,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    viewModel.stopScan()
                    navController.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    val encoded = Uri.encode(deviceAddress)
                    navController.navigate("chart_screen/$encoded")
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.graph),
                    contentDescription = "Graph"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}