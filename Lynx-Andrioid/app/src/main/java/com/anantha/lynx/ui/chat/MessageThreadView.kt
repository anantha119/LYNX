package com.anantha.lynx.ui.chat

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.model.ChatMessage
import com.anantha.lynx.ui.theme.LynxColor
import com.anantha.lynx.ui.theme.LynxFont
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Ported from Lynx-IOS/Lynx-IOS/Features/Chat/MessageThreadView.swift —
 * auto-scrolls to bottom on new content, triggers onLoadOlder when the top
 * sentinel appears (infinite-scroll-up cursor pagination).
 */
@Composable
fun MessageThreadView(
    messages: List<ChatMessage>,
    authState: AuthState,
    hasMore: Boolean,
    loadingOlder: Boolean,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(listState, hasMore) {
        androidx.compose.runtime.snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40
        }.distinctUntilChanged().collect { atTop ->
            if (atTop && hasMore) onLoadOlder()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
    ) {
        if (loadingOlder) {
            item(key = "loading-older") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text("Loading earlier messages…", style = LynxFont.mono(10.sp), color = LynxColor.stone600)
                }
            }
        }

        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message, authState = authState)
        }
    }
}
