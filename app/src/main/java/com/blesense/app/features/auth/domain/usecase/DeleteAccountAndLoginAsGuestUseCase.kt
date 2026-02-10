package com.blesense.app.features.auth.domain.usecase

import com.blesense.app.features.auth.domain.model.User

class DeleteAccountAndLoginAsGuestUseCase(
    private val deleteAccount: DeleteAccountUseCase,
    private val guestLogin: GuestLoginUseCase
) {
    suspend operator fun invoke(password: String?): User {
        deleteAccount(password)
        return guestLogin()
    }
}
