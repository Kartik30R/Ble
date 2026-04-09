package com.blesense.app.features.settings.presentation.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blesense.app.features.settings.domain.model.SettingsItem
import com.blesense.app.features.settings.domain.model.SettingsItemType
import com.blesense.app.coreui.theme.*
import com.blesense.app.coreui.components.*

@Composable
fun SettingsOptionsList(
    cardBackground: Color,
    textColor: Color,
    secondaryTextColor: Color,
    dividerColor: Color,
    iconTint: Color,
    navController: NavHostController
) {

    val settingsOptions = listOf(
        SettingsItem(Icons.AutoMirrored.Outlined.Help, "Help", SettingsItemType.DETAIL),
        SettingsItem(Icons.Outlined.AccountCircle, "Accounts", SettingsItemType.DETAIL),
        SettingsItem(Icons.Outlined.Info, "About BLE", SettingsItemType.DETAIL),
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column {

            settingsOptions.forEachIndexed { index, item ->

                SettingsItemRow(
                    item = item,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    iconTint = iconTint,
                    navController = navController
                )

                if (index < settingsOptions.size - 1) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}