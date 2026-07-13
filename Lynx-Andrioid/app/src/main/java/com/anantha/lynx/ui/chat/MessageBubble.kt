package com.anantha.lynx.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.model.ChatMessage
import com.anantha.lynx.model.Role
import com.anantha.lynx.ui.conversations.AvatarView
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMark
import com.anantha.lynx.ui.theme.LynxMetrics
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Chat/MessageBubble.swift — assistant
 * messages left-aligned with an amber rule, user bubbles right-aligned in a
 * stone box capped to ~72% of the thread's width.
 */
@Composable
fun MessageBubble(message: ChatMessage, authState: AuthState, modifier: Modifier = Modifier) {
    val isAssistant = message.role == Role.assistant

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (isAssistant) {
            LynxMark(size = 18.dp)
            BubbleStack(message = message, isAssistant = true, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(24.dp))
        } else {
            Spacer(Modifier.weight(1f, fill = false))
            BoxWithConstraints {
                BubbleStack(
                    message = message,
                    isAssistant = false,
                    modifier = Modifier.widthIn(max = maxWidth * 0.72f),
                )
            }
            AvatarView(authState = authState, size = 22.dp)
        }
    }
}

@Composable
private fun BubbleStack(message: ChatMessage, isAssistant: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = if (isAssistant) Alignment.Start else Alignment.End,
        modifier = modifier,
    ) {
        if (isAssistant) {
            Row {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(LynxColor.amber.copy(alpha = 0.6f)),
                )
                Spacer(Modifier.width(10.dp))
                MarkdownMessage(content = message.content, streaming = message.streaming)
            }
        } else {
            Text(
                message.content,
                style = LynxFont.mono(14.sp),
                color = LynxColor.stone100,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .background(LynxColor.stone800, RoundedCornerShape(LynxMetrics.cornerRadius))
                    .border(1.dp, LynxColor.stone700, RoundedCornerShape(LynxMetrics.cornerRadius))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        Text(
            timeFormatter.format(message.timestamp),
            style = LynxFont.mono(9.sp),
            color = LynxColor.stone700,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
