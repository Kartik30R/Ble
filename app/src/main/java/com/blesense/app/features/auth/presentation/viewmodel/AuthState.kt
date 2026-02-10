package com.blesense.app.features.auth.presentation.viewmodel

import com.blesense.app.features.auth.domain.model.User

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
    data object PasswordResetEmailSent : AuthState
    data object AccountDeleted : AuthState
}