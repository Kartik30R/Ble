package com.blesense.app.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.blesense.app.core.di.AuthModule
import com.blesense.app.features.auth.presentation.viewmodel.AuthViewModel

class AuthViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(
                loginUseCase = AuthModule.loginUseCase,
                registerUseCase = AuthModule.registerUseCase,
                guestLoginUseCase = AuthModule.guestLoginUseCase,
                googleLoginUseCase = AuthModule.googleLoginUseCase,
                sendPasswordResetUseCase = AuthModule.sendPasswordResetUseCase,
                logoutUseCase = AuthModule.logoutUseCase,
                deleteAndGuestUseCase = AuthModule.deleteAccountAndLoginAsGuestUseCase,
                getCurrentUserUseCase = AuthModule.getCurrentUserUseCase,
                isUserAuthenticatedUseCase = AuthModule.isUserAuthenticatedUseCase
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}