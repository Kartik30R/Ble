package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.repository.AuthRepository

class DeleteAccountUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(password: String? = null) {
        repository.deleteAccount(password)
    }
}
