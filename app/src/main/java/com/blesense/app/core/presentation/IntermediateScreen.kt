package com.blesense.app.core.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blesense.app.R
import com.blesense.app.features.remote.RobotControlCompose
import com.blesense.app.coreui.theme.BleSenseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntermediateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { /* Handle result */ }
    )

    BleSenseTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            text = "BLE Sense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        text = "Choose a Destination",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // First row: Bluetooth and Data Logger
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NavigationIconBox(
                            iconResId = R.drawable.bluetooth,
                            label = "Bluetooth",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("home_screen") }
                        )

                        NavigationIconBox(
                            iconResId = R.drawable.data_logger,
                            label = "Data Logger",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate("data_logger/auto_connect/DataLogger/DataLogger_1")
                            }
                        )
                    }
                }

                // Second row: Robot Control and Settings
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NavigationIconBox(
                            iconResId = R.drawable.robo_car_icon,
                            label = "Robot Control",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(context, RobotControlCompose::class.java)
                                launcher.launch(intent)
                            }
                        )

                        NavigationIconBox(
                            iconResId = R.drawable.settings,
                            label = "Settings",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("settings_screen") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationIconBox(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Use Centralized Theme for gradient and colors
    val primaryGradient = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        label = "elevation"
    )

    ElevatedCard(
        modifier = modifier
            .aspectRatio(1.1f)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isPressed) MaterialTheme.colorScheme.surfaceVariant else containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Icon with brand gradient background
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(primaryGradient, primaryGradient.copy(alpha = 0.8f)),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = onSurfaceColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}