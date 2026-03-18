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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blesense.app.features.settings.domain.model.SettingsItem
import com.blesense.app.features.settings.domain.model.SettingsItemType

@Composable
fun SettingsOptionsList(
    cardBackground: Color,
    textColor: Color,
    secondaryTextColor: Color,
    dividerColor: Color,
    iconTint: Color,
    isDarkMode: Boolean,
    isLiveStreaming: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onLiveStreamingToggle: (Boolean) -> Unit,
    navController: NavHostController
) {

    val settingsOptions = listOf(
        SettingsItem(Icons.Outlined.DarkMode, "Dark Mode", SettingsItemType.SWITCH),
        SettingsItem(Icons.Outlined.Cloud, "Live Streaming", SettingsItemType.SWITCH),
        SettingsItem(Icons.AutoMirrored.Outlined.Help, "Help", SettingsItemType.DETAIL),
        SettingsItem(Icons.Outlined.AccountCircle, "Accounts", SettingsItemType.DETAIL),
        SettingsItem(Icons.Outlined.Info, "About BLE", SettingsItemType.DETAIL),
    )

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column {

            settingsOptions.forEachIndexed { index, item ->

                when (item.title) {

                    "Dark Mode" -> {
                        SettingsItemRow(
                            item = item,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            iconTint = iconTint,
                            initialSwitchState = isDarkMode,
                            onSwitchChange = onDarkModeToggle,
                            navController = navController
                        )
                    }

                    "Live Streaming" -> {
                        SettingsItemRow(
                            item = item,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            iconTint = iconTint,
                            initialSwitchState = isLiveStreaming,
                            onSwitchChange = onLiveStreamingToggle,
                            navController = navController
                        )
                    }

                    else -> {
                        SettingsItemRow(
                            item = item,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            iconTint = iconTint,
                            navController = navController
                        )
                    }
                }

                if (index < settingsOptions.size - 1) {
                    Divider(
                        color = dividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}