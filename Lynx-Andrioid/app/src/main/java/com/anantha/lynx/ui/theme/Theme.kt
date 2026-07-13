package com.anantha.lynx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Lynx is dark-only (mirrors RootView.swift's `.preferredColorScheme(.dark)`)
 * — no light theme / dynamic color branching, unlike the default template.
 */
private val LynxDarkColorScheme = darkColorScheme(
    primary = LynxColor.amberBright,
    secondary = LynxColor.amber,
    tertiary = LynxColor.amberBright,
    background = LynxColor.bg,
    surface = LynxColor.panelBg,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = LynxColor.stone100,
    onSurface = LynxColor.stone100,
)

@Composable
fun LynxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LynxDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
