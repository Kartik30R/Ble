package com.blesense.app.features.auth.domain.model

sealed class AuthFailure {
    data object InvalidCredentials : AuthFailure()
    data object EmailAlreadyExists : AuthFailure()
    data object WeakPassword : AuthFailure()
    data object RecentLoginRequired : AuthFailure()
    data class Unknown(val message: String?) : AuthFailure()
}
