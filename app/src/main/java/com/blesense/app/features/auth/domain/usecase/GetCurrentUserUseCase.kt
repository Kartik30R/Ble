package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.model.User
import com.blesense.app.features.auth.domain.repository.AuthRepository

class GetCurrentUserUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): User? {
        return repository.getCurrentUser()
    }
}
