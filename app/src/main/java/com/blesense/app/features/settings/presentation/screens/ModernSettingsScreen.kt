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
import com.blesense.app.coreui.theme.ThemeManager
import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.blesense.app.features.settings.presentation.widgets.PrivacyPolicyButton
import com.blesense.app.features.settings.presentation.widgets.SettingsOptionsList
import com.blesense.app.features.settings.presentation.widgets.UserProfileCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsScreen(
    viewModel: AuthViewModel = viewModel(),
    onSignOut: () -> Unit,
    navController: NavHostController
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user
    val isDark by ThemeManager.isDarkMode.collectAsState()

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
                    IconButton(onClick = { navController.popBackStack() }) {
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

            /* ---------------- User Profile ---------------- */

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
                    viewModel.logout()
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

            /* ---------------- Settings Options ---------------- */

            SettingsOptionsList(
                cardBackground = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.onBackground,
                secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                dividerColor = MaterialTheme.colorScheme.outlineVariant,
                iconTint = MaterialTheme.colorScheme.primary,
                isDarkMode = isDark,
                onDarkModeToggle = {ThemeManager.toggleDarkMode(it) },
                navController = navController
            )

            Spacer(modifier = Modifier.height(16.dp))

            /* ---------------- Privacy Policy ---------------- */

            PrivacyPolicyButton()
        }
    }
}
