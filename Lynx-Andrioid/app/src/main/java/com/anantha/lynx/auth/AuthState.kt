package com.anantha.lynx.auth

import java.net.URI

/** Ported from Lynx-IOS/Lynx-IOS/Auth/AuthStore.swift's nested State/UserInfo types. */
sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: UserInfo) : AuthState()
}

data class UserInfo(
    val name: String,
    val email: String?,
    val pictureUrl: URI?,
)
