package com.anantha.lynx.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.auth.AuthStore
import com.anantha.lynx.ui.login.LoginScreen
import com.anantha.lynx.ui.theme.LynxColor

/**
 * Auth-gated root, ported from Lynx-IOS/Lynx-IOS/App/RootView.swift. Switches
 * between LoginScreen and the signed-in MainAppShell based on AuthStore.state.
 */
@Composable
fun RootScreen(activity: ComponentActivity, modifier: Modifier = Modifier) {
    val authStore: AuthStore = viewModel()
    val authState by authStore.state.collectAsStateWithLifecycle()
    val lastError by authStore.lastError.collectAsStateWithLifecycle()

    when (val state = authState) {
        is AuthState.Loading -> LoadingScreen(modifier)
        is AuthState.SignedOut -> LoginScreen(
            lastError = lastError,
            onLogin = { authStore.login(activity) },
            modifier = modifier,
        )
        is AuthState.SignedIn -> MainAppShell(
            authStore = authStore,
            activity = activity,
            user = state.user,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(LynxColor.bg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LynxColor.amberBright)
    }
}
