package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.repository.AuthRepository

class IsUserAuthenticatedUseCase(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return repository.isUserAuthenticated()
    }
}
