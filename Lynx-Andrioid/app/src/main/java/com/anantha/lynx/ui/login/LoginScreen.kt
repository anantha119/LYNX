package com.anantha.lynx.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMark
import com.anantha.lynx.ui.theme.LynxMetrics

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Login/LoginView.swift — dark hero
 * screen with a dot-grid background, amber radial glow, wordmark, and a
 * single Auth0 login button.
 */
@Composable
fun LoginScreen(lastError: String?, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    var isSigningIn by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(LynxColor.bg)) {
        DotGridBackground(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(LynxColor.amber.copy(alpha = 0.10f), Color.Transparent),
                        radius = 780f,
                    )
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                LynxMark(size = 40.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    "LYNX",
                    style = LynxFont.display(22.sp, FontWeight.Bold),
                    color = LynxColor.stone100,
                    letterSpacing = 4.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "What can I help\nyou ship?",
                    style = LynxFont.display(30.sp, FontWeight.Bold),
                    color = LynxColor.stone100,
                    textAlign = TextAlign.Center,
                )

                if (lastError != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        lastError,
                        style = LynxFont.mono(11.sp),
                        color = Color.Red.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .background(LynxColor.amberBright, RoundedCornerShape(LynxMetrics.cornerRadius))
                    .clickable(enabled = !isSigningIn) {
                        isSigningIn = true
                        onLogin()
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.width(16.dp).height(16.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSigningIn) "Signing in…" else "Continue with Auth0",
                    style = LynxFont.mono(13.sp, FontWeight.Medium),
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun DotGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val spacing = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = 0.7.dp.toPx(),
                    center = Offset(x, y),
                )
                y += spacing
            }
            x += spacing
        }
    }
}
