package com.anantha.lynx.network

/**
 * Ported from Lynx-IOS/Lynx-IOS/Networking/AppConfig.swift.
 * Mirrors NEXT_PUBLIC_API_URL in the web client (chat-app.tsx). Pointed at
 * the deployed Cloud Run backend so no local server is needed.
 */
object AppConfig {
    const val API_BASE_URL = "https://lynx-backend-waw7tiae6q-uc.a.run.app"

    const val AUTH0_AUDIENCE = "https://api.lynx.app"
    const val AUTH0_SCOPE = "openid profile email offline_access"
}
