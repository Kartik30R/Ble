package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke() {
        repository.logout()
    }
}
