package com.anantha.lynx.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.anantha.lynx.app.ErrorBanner
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import com.anantha.lynx.ui.theme.LynxMetrics

private data class ActionChip(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val prompt: String)

private val actionChips = listOf(
    ActionChip(Icons.Filled.CameraAlt, "Clone a Screenshot", "Clone a screenshot for me"),
    ActionChip(Icons.Filled.Add, "Import from Figma", "Import a Figma design"),
    ActionChip(Icons.Filled.Upload, "Upload a Project", "Upload and analyze my project"),
)

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Chat/ChatScreen.swift — top bar,
 * either the message thread + input or the hero empty state + input, over
 * a dark radial-glow background.
 */
@Composable
fun ChatScreen(
    conversationTitle: String?,
    hasMessages: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    onOpenSidebar: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    messageThread: @Composable () -> Unit = {},
) {
    var draft by remember { mutableStateOf("") }

    fun sendDraft() {
        val trimmed = draft.trim()
        if (trimmed.isEmpty()) return
        draft = ""
        onSend(trimmed)
    }

    Box(modifier = modifier.fillMaxSize().background(LynxColor.bg)) {
        // Background fills edge-to-edge; content respects the status bar /
        // gesture nav insets (mirrors iOS's bg.ignoresSafeArea() + content
        // still laid out within the safe area).
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopBar(conversationTitle = conversationTitle, hasMessages = hasMessages, onOpenSidebar = onOpenSidebar)

            if (errorMessage != null) {
                ErrorBanner(message = errorMessage, onDismiss = onDismissError, modifier = Modifier.padding(top = 8.dp))
            }

            if (hasMessages) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { messageThread() }
                ChatInputBar(
                    text = draft,
                    onTextChange = { draft = it },
                    onSend = ::sendDraft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
                HeroEmptyState(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSend = ::sendDraft,
                    onChipTap = { prompt -> onSend(prompt) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TopBar(conversationTitle: String?, hasMessages: Boolean, onOpenSidebar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 18.dp, bottom = 12.dp),
    ) {
        IconButton(onClick = onOpenSidebar) {
            Icon(Icons.Filled.Menu, contentDescription = "Open conversations", tint = LynxColor.stone500)
        }
        if (conversationTitle != null) {
            Box(modifier = androidx.compose.ui.Modifier.width(1.dp).height(14.dp).background(LynxColor.amberBright))
            Spacer(modifier = androidx.compose.ui.Modifier.width(10.dp))
            Text(
                conversationTitle.uppercase(),
                style = LynxFont.mono(11.sp, FontWeight.Medium),
                color = LynxColor.stone200,
                maxLines = 1,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun HeroEmptyState(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onChipTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(LynxColor.amber.copy(alpha = 0.08f), Color.Transparent),
                        radius = 660f,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "What can I help",
                    style = LynxFont.display(28.sp, FontWeight.Bold),
                    color = LynxColor.stone100,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "you ship?",
                    style = LynxFont.display(28.sp, FontWeight.Bold),
                    color = LynxColor.amberBright,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChatInputBar(text = draft, onTextChange = onDraftChange, onSend = onSend, modifier = Modifier.fillMaxWidth())

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actionChips.forEach { chip ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onChipTap(chip.prompt) }
                            .background(LynxColor.panelBg, RoundedCornerShape(LynxMetrics.cornerRadius))
                            .border(1.dp, LynxColor.stone800, RoundedCornerShape(LynxMetrics.cornerRadius))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(chip.icon, contentDescription = null, tint = LynxColor.stone500, modifier = Modifier.width(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(chip.label, style = LynxFont.mono(11.sp), color = LynxColor.stone500)
                    }
                }
            }
        }
    }
}
