package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.repository.AuthRepository

class SendPasswordResetUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String) {
        repository.sendPasswordResetEmail(email)
    }
}
