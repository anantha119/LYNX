package com.anantha.lynx.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.app.ErrorBanner
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.model.Conversation
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMark
import com.anantha.lynx.ui.theme.LynxMetrics
import java.time.Duration
import java.time.Instant

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Conversations/ConversationListView.swift
 * — drawer content: header, new-chat button, conversations grouped by
 * recency, user footer with logout.
 */
@Composable
fun ConversationListView(
    conversations: List<Conversation>,
    activeId: String?,
    authState: AuthState,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LynxColor.sidebarBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 16.dp),
        ) {
            LynxMark(size = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text("LYNX", style = LynxFont.display(15.sp, FontWeight.Bold), color = LynxColor.stone100, letterSpacing = 2.sp)
        }

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage, onDismiss = onDismissError, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .clickable(onClick = onNew)
                .dashedBorder(LynxColor.stone800, LynxMetrics.cornerRadius)
                .padding(vertical = 10.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = LynxColor.stone500, modifier = Modifier.width(12.dp))
            Spacer(Modifier.width(8.dp))
            Text("New conversation", style = LynxFont.mono(12.sp), color = LynxColor.stone500)
        }

        val groups = groupConversations(conversations)
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = LynxColor.stone800, modifier = Modifier.width(22.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    "No conversations yet.\nStart a new one above.",
                    style = LynxFont.mono(10.sp),
                    color = LynxColor.stone700,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                groups.forEach { (label, items) ->
                    item(key = "header-$label") {
                        Text(
                            label.uppercase(),
                            style = LynxFont.mono(9.sp, FontWeight.Medium),
                            color = LynxColor.stone700,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    items(items, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            isActive = conversation.id == activeId,
                            onClick = { onSelect(conversation.id) },
                        )
                    }
                }
            }
        }

        UserFooter(authState = authState, onLogout = onLogout)
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, isActive: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isActive) LynxColor.stone900.copy(alpha = 0.8f) else Color.Transparent,
                RoundedCornerShape(LynxMetrics.cornerRadius),
            )
            .padding(vertical = 8.dp)
            .padding(start = 12.dp, end = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(if (isActive) LynxColor.amberBright else Color.Transparent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            conversation.title,
            style = LynxFont.mono(13.sp),
            color = if (isActive) LynxColor.stone100 else LynxColor.stone400,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = LynxColor.amberBright, modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun UserFooter(authState: AuthState, onLogout: () -> Unit) {
    val displayName = (authState as? AuthState.SignedIn)?.user?.name ?: "User"

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LynxColor.stone900))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            AvatarView(authState = authState, size = 26.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = LynxFont.mono(12.sp), color = LynxColor.stone300, maxLines = 1)
                Text("Free plan", style = LynxFont.mono(9.sp), color = LynxColor.stone700)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = LynxColor.stone700)
            }
        }
    }
}

// MARK: - Time grouping (mirrors chat-sidebar.tsx groupConversations)

private fun groupConversations(conversations: List<Conversation>): List<Pair<String, List<Conversation>>> {
    val now = Instant.now()
    val today = mutableListOf<Conversation>()
    val yesterday = mutableListOf<Conversation>()
    val week = mutableListOf<Conversation>()
    val older = mutableListOf<Conversation>()

    for (conv in conversations) {
        val days = Duration.between(conv.updatedAt, now).toHours() / 24.0
        when {
            days < 1 -> today.add(conv)
            days < 2 -> yesterday.add(conv)
            days < 7 -> week.add(conv)
            else -> older.add(conv)
        }
    }

    return listOf(
        "Today" to today,
        "Yesterday" to yesterday,
        "Past 7 days" to week,
        "Older" to older,
    ).filter { it.second.isNotEmpty() }
}

/** Dashed outline, mirroring the "New conversation" button's StrokeStyle(dash: [4, 3]) on iOS. */
private fun Modifier.dashedBorder(color: Color, cornerRadius: androidx.compose.ui.unit.Dp): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
    )
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke,
    )
}
