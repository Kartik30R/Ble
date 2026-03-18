package com.blesense.app.features.settings.presentation.widgets

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
 import com.blesense.app.coreui.theme.Helvetica
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel
import com.blesense.app.features.settings.domain.model.SettingsItem
import com.blesense.app.features.settings.domain.model.SettingsItemType
import com.blesense.app.features.settings.presentation.dialogs.AccountsDialog


@Composable
fun SettingsItemRow(
    item: SettingsItem,
    textColor: Color,
    secondaryTextColor: Color,
    iconTint: Color,
    initialSwitchState: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    navController: NavHostController,
) {
    var switchState by remember { mutableStateOf(initialSwitchState) }

    LaunchedEffect(initialSwitchState) {
        switchState = initialSwitchState
    }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAccountsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val isAboutItem = item.icon == Icons.Outlined.Info
    val isHelpItem = item.icon == Icons.AutoMirrored.Outlined.Help
    val isAccountsItem = item.icon == Icons.Outlined.AccountCircle

    /* ---------------- Row ---------------- */

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.type == SettingsItemType.DETAIL) {
                when {
                    isAboutItem -> showAboutDialog = true
                    isHelpItem -> showHelpDialog = true
                    isAccountsItem -> showAccountsDialog = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = Helvetica
            ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        when (item.type) {

            SettingsItemType.SWITCH -> {
                Switch(
                    checked = switchState,
                    onCheckedChange = {
                        switchState = it
                        onSwitchChange?.invoke(it)
                    }
                )
            }

            SettingsItemType.DETAIL -> {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Navigate",
                    tint = secondaryTextColor
                )
            }
        }
    }

    /* ---------------- About Dialog ---------------- */

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Bluetooth Low Energy") },
            text = {
                Text(
                    "Bluetooth Low Energy (BLE) is a low-power wireless technology " +
                            "designed for efficient communication."
                )
            }
        )
    }

    /* ---------------- Help Dialog ---------------- */

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:awadhropar@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Help/Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                        showHelpDialog = false
                    }
                ) {
                    Text("Contact Developer")
                }
            },
            title = { Text("Help & Support") },
            text = { Text("Need help? Contact the developer.") }
        )
    }

    /* ---------------- Accounts Dialog Trigger ---------------- */

    if (showAccountsDialog) {
        val authViewModel: AuthViewModel = viewModel()

        AccountsDialog(
             onDismiss = { showAccountsDialog = false },
            navController = navController,
            viewModel = authViewModel
        )
    }
}
