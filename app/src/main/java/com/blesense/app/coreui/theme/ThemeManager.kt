package com.blesense.app.coreui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeManager {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private var isInitialized = false

    fun toggleDarkMode(value: Boolean) {
        _isDarkMode.value = value
        isInitialized = true
    }

    fun initializeWithSystemTheme(isSystemDark: Boolean) {
        if (!isInitialized) {
            _isDarkMode.value = isSystemDark
            isInitialized = true
        }
    }
}
