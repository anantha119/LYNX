package com.anantha.lynx.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anantha.lynx.R

/**
 * The amber sparkle mark used throughout the app (login hero, avatar
 * fallback, assistant message rows) — ported from LoginView.swift's
 * `LynxMark` struct.
 */
@Composable
fun LynxMark(size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_lynx_mark),
        contentDescription = null,
        tint = LynxColor.amber,
        modifier = modifier.size(size),
    )
}
