package com.blesense.app.features.auth.data.entity

import com.google.firebase.auth.FirebaseUser

data class AuthUserDto(
    val id: String,
    val email: String?,
    val isAnonymous: Boolean,
    val name: String?,
    val photoUrl: String?
)

fun FirebaseUser.toDto(): AuthUserDto =
    AuthUserDto(
        id = uid,
        email = email,
        isAnonymous = isAnonymous,
        name = displayName ?: email?.substringBefore('@'),
        photoUrl = photoUrl?.toString()
    )
