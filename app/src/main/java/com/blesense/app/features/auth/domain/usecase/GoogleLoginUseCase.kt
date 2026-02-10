package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.model.User
import com.blesense.app.features.auth.domain.repository.AuthRepository

class GoogleLoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        idToken: String
    ): User {
        return repository.loginWithGoogle(idToken)
    }
}
