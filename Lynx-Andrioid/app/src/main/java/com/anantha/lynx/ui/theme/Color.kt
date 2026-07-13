package com.anantha.lynx.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens mirroring the Lynx web app's dark "terminal" aesthetic
 * (see lynx/src/app/globals.css and the stone/amber palette used across
 * chat-app.tsx, chat-sidebar.tsx, chat-messages.tsx, v0-ai-chat.tsx).
 * Ported 1:1 from Lynx-IOS/Lynx-IOS/App/Theme.swift's LynxColor enum.
 */
object LynxColor {
    val amber = Color(0xFFF59E0B)
    val amberBright = Color(0xFFFCBF24) // amber-400

    val bg = Color(0xFF080808)
    val sidebarBg = Color(0xFF060606)
    val panelBg = Color(0xFF0E0E0E)
    val codeBg = Color(0xFF0A0A0A)

    // stone-* approximations (Tailwind stone scale)
    val stone100 = Color(red = 0.96f, green = 0.96f, blue = 0.96f)
    val stone200 = Color(red = 0.90f, green = 0.90f, blue = 0.90f)
    val stone300 = Color(red = 0.80f, green = 0.80f, blue = 0.80f)
    val stone400 = Color(red = 0.65f, green = 0.65f, blue = 0.65f)
    val stone500 = Color(red = 0.50f, green = 0.50f, blue = 0.50f)
    val stone600 = Color(red = 0.38f, green = 0.38f, blue = 0.38f)
    val stone700 = Color(red = 0.28f, green = 0.26f, blue = 0.24f)
    val stone800 = Color(red = 0.19f, green = 0.18f, blue = 0.16f)
    val stone900 = Color(red = 0.11f, green = 0.10f, blue = 0.09f)
}
