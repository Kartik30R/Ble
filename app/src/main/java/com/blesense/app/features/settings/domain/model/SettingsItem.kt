package com.blesense.app.features.settings.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val type: SettingsItemType
)

enum class SettingsItemType {
    SWITCH,
    DETAIL
}