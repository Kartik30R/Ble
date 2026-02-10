package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.model.User
import com.blesense.app.features.auth.domain.repository.AuthRepository

class GuestLoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): User {
        return repository.loginAsGuest()
    }
}
