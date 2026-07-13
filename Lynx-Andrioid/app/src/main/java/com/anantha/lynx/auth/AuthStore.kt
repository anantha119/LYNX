package com.anantha.lynx.auth

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anantha.lynx.R
import com.anantha.lynx.network.AppConfig
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.auth0.android.jwt.JWT
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps the Auth0 Android SDK's native PKCE flow + encrypted credential
 * storage/refresh. Ported from Lynx-IOS/Lynx-IOS/Auth/AuthStore.swift.
 *
 * Unlike iOS's `Auth0.webAuth().start()` (no UI context needed), Android's
 * WebAuthProvider must be launched from an Activity, so login()/logout()
 * take a ComponentActivity — the one Android-specific deviation from the
 * iOS call shape.
 */
class AuthStore(application: Application) : AndroidViewModel(application) {

    private val account = Auth0.getInstance(
        application.getString(R.string.com_auth0_client_id),
        application.getString(R.string.com_auth0_domain),
    )

    private val credentialsManager = SecureCredentialsManager(
        application,
        account,
        SharedPreferencesStorage(application),
    )

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        viewModelScope.launch { restoreSession() }
    }

    private suspend fun restoreSession() {
        if (!credentialsManager.hasValidCredentials()) {
            _state.value = AuthState.SignedOut
            return
        }
        try {
            val credentials = credentialsManager.awaitCredentials()
            _state.value = AuthState.SignedIn(userInfo(credentials))
        } catch (e: Exception) {
            _state.value = AuthState.SignedOut
        }
    }

    fun login(activity: ComponentActivity) {
        viewModelScope.launch {
            _lastError.value = null
            try {
                val credentials = suspendCancellableCoroutine<Credentials> { continuation ->
                    WebAuthProvider.login(account)
                        .withScheme(activity.packageName)
                        .withAudience(AppConfig.AUTH0_AUDIENCE)
                        .withScope(AppConfig.AUTH0_SCOPE)
                        .start(activity, object : Callback<Credentials, AuthenticationException> {
                            override fun onSuccess(result: Credentials) {
                                continuation.resume(result)
                            }

                            override fun onFailure(error: AuthenticationException) {
                                continuation.resumeWithException(error)
                            }
                        })
                }
                credentialsManager.saveCredentials(credentials)
                _state.value = AuthState.SignedIn(userInfo(credentials))
            } catch (e: Exception) {
                _lastError.value = describe(e)
                _state.value = AuthState.SignedOut
            }
        }
    }

    /**
     * Surfaces the underlying Auth0 error detail rather than a generic
     * message, since a failed exchange (bad audience/scope, callback
     * mismatch, etc.) is otherwise indistinguishable from a user cancel.
     */
    private fun describe(e: Exception): String =
        (e as? AuthenticationException)?.getDescription() ?: e.message ?: "Sign-in failed."

    fun logout(activity: ComponentActivity) {
        viewModelScope.launch {
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    WebAuthProvider.logout(account)
                        .withScheme(activity.packageName)
                        .start(activity, object : Callback<Void?, AuthenticationException> {
                            override fun onSuccess(result: Void?) {
                                continuation.resume(Unit)
                            }

                            override fun onFailure(error: AuthenticationException) {
                                continuation.resumeWithException(error)
                            }
                        })
                }
            } catch (e: Exception) {
                // Local sign-out proceeds regardless of remote session clear.
            }
            credentialsManager.clearCredentials()
            _state.value = AuthState.SignedOut
        }
    }

    /**
     * Returns a valid access token, transparently refreshing if needed
     * (used as the Bearer token against the Cloud Run backend).
     */
    suspend fun accessToken(): String = credentialsManager.awaitCredentials().accessToken

    private fun userInfo(credentials: Credentials): UserInfo {
        val jwt = JWT(credentials.idToken)
        val name = jwt.getClaim("name").asString() ?: jwt.getClaim("nickname").asString() ?: "User"
        val email = jwt.getClaim("email").asString()
        val picture = jwt.getClaim("picture").asString()?.let { runCatching { URI(it) }.getOrNull() }
        return UserInfo(name = name, email = email, pictureUrl = picture)
    }
}
