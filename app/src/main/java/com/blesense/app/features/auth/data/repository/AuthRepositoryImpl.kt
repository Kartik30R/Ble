package com.blesense.app.features.auth.data.repository

import com.blesense.app.features.auth.data.datasource.FirebaseAuthDataSource
import com.blesense.app.features.auth.data.entity.AuthUserDto
import com.blesense.app.features.auth.domain.model.AuthFailure
import com.blesense.app.features.auth.domain.model.User
import com.blesense.app.features.auth.domain.repository.AuthRepository
import com.google.firebase.auth.*

class AuthRepositoryImpl(
    private val dataSource: FirebaseAuthDataSource
) : AuthRepository {

    override fun getCurrentUser(): User? =
        dataSource.getCurrentUser()?.toDomain()

    override fun isUserAuthenticated(): Boolean =
        dataSource.getCurrentUser() != null

    override suspend fun login(email: String, password: String): User =
        safeCall { dataSource.login(email, password).toDomain() }

    override suspend fun register(email: String, password: String): User =
        safeCall { dataSource.register(email, password).toDomain() }

    override suspend fun loginWithGoogle(idToken: String): User =
        safeCall { dataSource.loginWithGoogle(idToken).toDomain() }

    override suspend fun loginAsGuest(): User =
        safeCall { dataSource.loginAsGuest().toDomain() }

    override suspend fun sendPasswordResetEmail(email: String) =
        dataSource.sendPasswordResetEmail(email)

    override suspend fun deleteAccount(password: String?) =
        safeCall { dataSource.deleteCurrentUser(password) }

    override fun logout() =
        dataSource.signOut()

    // ---------------------

    private fun AuthUserDto.toDomain() = User(
        id = id,
        email = email,
        isAnonymous = isAnonymous,
        name = name ?: "User",
        profilePictureUrl = photoUrl
    )

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            throw mapError(e)
        }
    }

    private fun mapError(e: Exception): RuntimeException {
        val failure = when (e) {
            is FirebaseAuthInvalidCredentialsException -> AuthFailure.InvalidCredentials
            is FirebaseAuthUserCollisionException -> AuthFailure.EmailAlreadyExists
            is FirebaseAuthWeakPasswordException -> AuthFailure.WeakPassword
            is FirebaseAuthRecentLoginRequiredException -> AuthFailure.RecentLoginRequired
            else -> AuthFailure.Unknown(e.message)
        }
        return RuntimeException(failure.toString())
    }
}
