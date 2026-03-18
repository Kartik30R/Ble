package com.blesense.app.features.settings.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import com.blesense.app.core.di.BluetoothModule
import com.blesense.app.core.network.LiveStreamController
import com.blesense.app.coreui.theme.ThemeManager

import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel

import com.blesense.app.features.settings.presentation.widgets.PrivacyPolicyButton
import com.blesense.app.features.settings.presentation.widgets.SettingsOptionsList
import com.blesense.app.features.settings.presentation.widgets.UserProfileCard

import presentation.viewmodel.BluetoothScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsScreen(
    navController: NavHostController,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {

    /* ---------- Bluetooth ViewModel (via Factory) ---------- */

    val bluetoothViewModel: BluetoothScanViewModel = viewModel(
        factory = BluetoothModule.bluetoothScanViewModelFactory()
    )

    /* ---------- Auth State ---------- */

    val authState by authViewModel.authState.collectAsState()

    val currentUser = (authState as? AuthState.Success)?.user

    /* ---------- UI State ---------- */

    val isDark by ThemeManager.isDarkMode.collectAsState()

    val wsEnabled by LiveStreamController.wsEnabled.collectAsState()

    /* ---------- UI ---------- */

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            /* ---------- User Profile ---------- */

            UserProfileCard(
                cardBackground = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onBackground,
                secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconTint = MaterialTheme.colorScheme.primary,

                userName = when {
                    currentUser?.isAnonymous == true -> "Guest User"
                    currentUser != null -> currentUser.email?.substringBefore('@') ?: "User"
                    else -> "Not Signed In"
                },

                userEmail = when {
                    currentUser?.isAnonymous == true -> "Anonymous User"
                    currentUser != null -> currentUser.email ?: ""
                    else -> ""
                },

                profilePictureUrl = currentUser?.profilePictureUrl,

                onLogout = {
                    authViewModel.logout()
                    onSignOut()
                }
            )

            if (authState is AuthState.Loading) {

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            /* ---------- Settings Options ---------- */

            SettingsOptionsList(
                cardBackground = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onBackground,
                secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                dividerColor = MaterialTheme.colorScheme.outlineVariant,
                iconTint = MaterialTheme.colorScheme.primary,

                isDarkMode = isDark,
                isLiveStreaming = wsEnabled,

                onDarkModeToggle = {
                    ThemeManager.toggleDarkMode(it)
                },

                onLiveStreamingToggle = { enabled ->

                    if (enabled) {

                        LiveStreamController.enableWebSocket()

                        bluetoothViewModel.connectWebSocket()

                    } else {

                        LiveStreamController.disableWebSocket()

                        bluetoothViewModel.disconnectWebSocket()
                    }
                },

                navController = navController
            )

            Spacer(modifier = Modifier.height(16.dp))

            /* ---------- Privacy Policy ---------- */

            PrivacyPolicyButton()
        }
    }
}