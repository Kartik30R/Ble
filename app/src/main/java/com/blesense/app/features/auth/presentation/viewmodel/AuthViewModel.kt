package com.blesense.app.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blesense.app.features.auth.domain.model.User
import com.blesense.app.features.auth.domain.usecase.DeleteAccountAndLoginAsGuestUseCase
import com.blesense.app.features.auth.domain.usecase.GetCurrentUserUseCase
import com.blesense.app.features.auth.domain.usecase.GoogleLoginUseCase
import com.blesense.app.features.auth.domain.usecase.GuestLoginUseCase
import com.blesense.app.features.auth.domain.usecase.IsUserAuthenticatedUseCase
import com.blesense.app.features.auth.domain.usecase.LoginUseCase
import com.blesense.app.features.auth.domain.usecase.LogoutUseCase
import com.blesense.app.features.auth.domain.usecase.RegisterUseCase
import com.blesense.app.features.auth.domain.usecase.SendPasswordResetUseCase
import com.blesense.app.features.auth.presentation.viewmodel.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val guestLoginUseCase: GuestLoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAndGuestUseCase: DeleteAccountAndLoginAsGuestUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val isUserAuthenticatedUseCase: IsUserAuthenticatedUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        val user = getCurrentUserUseCase()
        _authState.value = user?.let { AuthState.Success(it) } ?: AuthState.Idle
    }

    fun isUserAuthenticated(): Boolean =
        isUserAuthenticatedUseCase()

    fun login(email: String, password: String) =
        launchAuth { loginUseCase(email, password) }

    fun register(email: String, password: String) =
        launchAuth { registerUseCase(email, password) }

    fun loginAsGuest() =
        launchAuth { guestLoginUseCase() }

    fun loginWithGoogle(idToken: String) =
        launchAuth { googleLoginUseCase(idToken) }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            sendPasswordResetUseCase(email)
            _authState.value = AuthState.PasswordResetEmailSent
        }
    }

    fun deleteAccount(password: String?) =
        launchAuth { deleteAndGuestUseCase(password) }

    fun logout() {
        logoutUseCase()
        _authState.value = AuthState.Idle
    }

    private fun launchAuth(block: suspend () -> Any) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = block()
                _authState.value = AuthState.Success(user as User)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Something went wrong")
            }
        }
    }
}