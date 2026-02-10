package com.blesense.app.Presentation.widgets

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

@Composable
fun HeaderSection(
    navController: NavController,
    viewModel: BluetoothScanViewModel,
    deviceAddress: String,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button - triggers ViewModel cleanup and pops stack
        IconButton(
            onClick = {
                viewModel.stopScan()
                navController.popBackStack()
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }

        // Screen title using AppStrings
        Text(
            text = AppStrings.ADVERTISING_DATA_TITLE,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            style = MaterialTheme.typography.headlineMedium
        )

        // Chart navigation button
        IconButton(
            onClick = {
                 navController.navigate("chart_screen/$deviceAddress")
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.graph),
                contentDescription = "Graph Icon",
                modifier = Modifier.size(32.dp),
                tint = textColor
            )
        }
    }
}