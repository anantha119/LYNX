package com.anantha.lynx.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import com.anantha.lynx.auth.AuthState
import com.anantha.lynx.auth.AuthStore
import com.anantha.lynx.auth.UserInfo
import com.anantha.lynx.ui.chat.ChatScreen
import com.anantha.lynx.ui.chat.MessageThreadView
import com.anantha.lynx.ui.conversations.ConversationListView
import com.anantha.lynx.ui.conversations.ConversationStore
import kotlinx.coroutines.launch

private val SIDEBAR_WIDTH = 280.dp

/**
 * Signed-in shell — mirrors RootView.swift's MainAppView: ChatScreen behind
 * a scrim + custom drag-drawer ConversationListView, matching the iOS
 * drag-to-close-only behavior (30%-of-width release threshold, 250ms
 * ease-out) rather than a Material ModalNavigationDrawer.
 */
@Composable
fun MainAppShell(
    authStore: AuthStore,
    activity: ComponentActivity,
    user: UserInfo,
    modifier: Modifier = Modifier,
) {
    val conversationStore: ConversationStore = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ConversationStore(authStore) as T
        },
    )

    val conversations by conversationStore.conversations.collectAsStateWithLifecycle()
    val activeId by conversationStore.activeId.collectAsStateWithLifecycle()
    val messagesByConversation by conversationStore.messagesByConversation.collectAsStateWithLifecycle()
    val pageInfo by conversationStore.pageInfo.collectAsStateWithLifecycle()
    val loadingOlderId by conversationStore.loadingOlderId.collectAsStateWithLifecycle()
    val errorMessage by conversationStore.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { conversationStore.loadConversations() }

    var isSidebarOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val sidebarWidthPx = with(density) { SIDEBAR_WIDTH.toPx() }
    val offsetX = remember { Animatable(-sidebarWidthPx) }
    val scope = rememberCoroutineScope()

    fun openSidebar() {
        isSidebarOpen = true
        scope.launch { offsetX.animateTo(0f, tween(250)) }
    }

    fun closeSidebar() {
        scope.launch {
            offsetX.animateTo(-sidebarWidthPx, tween(250))
            isSidebarOpen = false
        }
    }

    val activeConversation = conversations.firstOrNull { it.id == activeId }
    val activeMessages = activeId?.let { messagesByConversation[it] } ?: emptyList()

    Box(modifier = modifier.fillMaxSize()) {
        ChatScreen(
            conversationTitle = activeConversation?.title,
            hasMessages = activeId != null && activeMessages.isNotEmpty(),
            errorMessage = errorMessage,
            onDismissError = conversationStore::dismissError,
            onOpenSidebar = ::openSidebar,
            onSend = { text -> conversationStore.send(text) },
            messageThread = {
                MessageThreadView(
                    messages = activeMessages,
                    authState = AuthState.SignedIn(user),
                    hasMore = activeId?.let(conversationStore::activeHasMore) ?: false,
                    loadingOlder = loadingOlderId == activeId,
                    onLoadOlder = { activeId?.let(conversationStore::loadOlder) },
                )
            },
        )

        AnimatedVisibility(visible = isSidebarOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { closeSidebar() },
            )
        }

        Box(
            modifier = Modifier
                .width(SIDEBAR_WIDTH)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value < -sidebarWidthPx * 0.3f) {
                                closeSidebar()
                            } else {
                                scope.launch { offsetX.animateTo(0f, tween(250)) }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newValue = (offsetX.value + dragAmount).coerceIn(-sidebarWidthPx, 0f)
                                offsetX.snapTo(newValue)
                            }
                        },
                    )
                },
        ) {
            ConversationListView(
                conversations = conversations,
                activeId = activeId,
                authState = AuthState.SignedIn(user),
                errorMessage = null,
                onDismissError = {},
                onSelect = { id ->
                    conversationStore.selectConversation(id)
                    closeSidebar()
                },
                onNew = {
                    conversationStore.startNewConversation()
                    closeSidebar()
                },
                onLogout = { authStore.logout(activity) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
