package com.blesense.app.core.di

 import com.blesense.app.features.auth.data.datasource.FirebaseAuthDataSource
import com.blesense.app.features.auth.data.repository.AuthRepositoryImpl
import com.blesense.app.features.auth.domain.repository.AuthRepository
import com.blesense.app.features.auth.domain.usecase.*
import com.google.firebase.auth.FirebaseAuth

object AuthModule {

    // -------- Firebase --------
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // -------- Data source --------
    private val authDataSource: FirebaseAuthDataSource by lazy {
        FirebaseAuthDataSource(firebaseAuth)
    }

    // -------- Repository --------
    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authDataSource)
    }

    // -------- UseCases --------
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository) }
    val guestLoginUseCase by lazy { GuestLoginUseCase(authRepository) }
    val googleLoginUseCase by lazy { GoogleLoginUseCase(authRepository) }
    val sendPasswordResetUseCase by lazy { SendPasswordResetUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val deleteAccountUseCase by lazy { DeleteAccountUseCase(authRepository) }

     val deleteAccountAndLoginAsGuestUseCase by lazy {
        DeleteAccountAndLoginAsGuestUseCase(
            deleteAccount = deleteAccountUseCase,
            guestLogin = guestLoginUseCase
        )
    }

    val getCurrentUserUseCase by lazy { GetCurrentUserUseCase(authRepository) }
    val isUserAuthenticatedUseCase by lazy { IsUserAuthenticatedUseCase(authRepository) }
}
