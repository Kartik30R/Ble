package com.blesense.app.core.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blesense.app.R
import com.blesense.app.features.remote.RobotControlCompose

private val CornerRadius = 28.dp
private val HorizontalPadding = 24.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntermediateScreen(navController: NavHostController) {

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BLE Sense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {

                Image(
                    painter = painterResource(R.drawable.logofinallast),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "Manage BLE devices and tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    ProfessionalCard(
                        icon = R.drawable.bluetooth,
                        title = "Bluetooth",
                        subtitle = "Scan and connect"
                    ) {
                        navController.navigate("home_screen")
                    }
                }


                item {
                    ProfessionalCard(
                        icon = R.drawable.data_logger,
                        title = "Data Logger",
                        subtitle = "Record sensor data"
                    ) {
                        navController.navigate("data_logger/auto_connect/DataLogger/DataLogger_1")
                    }
                }

                item {
                    ProfessionalCard(
                        icon = R.drawable.robo_car_icon,
                        title = "Robot Control",
                        subtitle = "Control robot remotely"
                    ) {
                        val intent = Intent(context, RobotControlCompose::class.java)
                        launcher.launch(intent)
                    }
                }


                item {
                    ProfessionalCard(
                        icon = R.drawable.settings,
                        title = "Settings",
                        subtitle = "App configuration"
                    ) {
                        navController.navigate("settings_screen")
                    }
                }
            }
        }
    }
}


@Composable
fun ProfessionalCard(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}