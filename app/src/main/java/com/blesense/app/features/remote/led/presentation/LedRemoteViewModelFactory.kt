package com.blesense.app.features.remote.led.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blesense.app.features.bluetooth.domain.usecase.SendBleCommandUseCase
import com.blesense.app.features.bluetooth.domain.usecase.StopBleAdvertisingUseCase

class LedRemoteViewModelFactory(
    private val sendBleCommand: SendBleCommandUseCase,
    private val stopBleAdvertising: StopBleAdvertisingUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedRemoteViewModel::class.java)) {
            return LedRemoteViewModel(sendBleCommand, stopBleAdvertising) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
