package com.blesense.app.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LiveStreamController {

    private val _wsEnabled = MutableStateFlow(false)

    val wsEnabled: StateFlow<Boolean> = _wsEnabled

    fun enableWebSocket() {
        _wsEnabled.value = true
    }

    fun disableWebSocket() {
        _wsEnabled.value = false
    }
}