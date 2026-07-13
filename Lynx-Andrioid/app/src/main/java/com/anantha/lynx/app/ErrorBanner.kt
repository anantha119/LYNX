package com.anantha.lynx.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMetrics
import kotlinx.coroutines.delay

/**
 * Self-dismissing banner for surfacing ConversationStore failures. Ported
 * from Lynx-IOS/Lynx-IOS/App/ErrorBanner.swift.
 */
@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(4000)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .background(LynxColor.panelBg, RoundedCornerShape(LynxMetrics.cornerRadius))
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(LynxMetrics.cornerRadius))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.85f),
                modifier = Modifier.width(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = LynxFont.mono(11.sp),
                color = LynxColor.stone200,
                modifier = Modifier.weight(1f, fill = true),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = LynxColor.stone500)
            }
        }
    }
}
