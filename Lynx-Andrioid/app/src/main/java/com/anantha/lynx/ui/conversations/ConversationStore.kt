package com.anantha.lynx.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anantha.lynx.auth.AuthStore
import com.anantha.lynx.model.ChatMessage
import com.anantha.lynx.model.Conversation
import com.anantha.lynx.model.Role
import com.anantha.lynx.model.SseEvent
import com.anantha.lynx.network.ApiClient
import com.anantha.lynx.network.SseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class PageInfo(val nextCursor: String?, val hasMore: Boolean)

/**
 * The main state machine, mirrors the web app's ChatApp (chat-app.tsx) via
 * iOS's ConversationStore.swift: token -> conversation list -> lazy-loaded
 * messages per conversation -> optimistic send + SSE token streaming ->
 * refresh list for title/reorder.
 */
class ConversationStore(private val authStore: AuthStore) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messagesByConversation = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messagesByConversation: StateFlow<Map<String, List<ChatMessage>>> = _messagesByConversation.asStateFlow()

    private val _pageInfo = MutableStateFlow<Map<String, PageInfo>>(emptyMap())
    val pageInfo: StateFlow<Map<String, PageInfo>> = _pageInfo.asStateFlow()

    private val _loadingOlderId = MutableStateFlow<String?>(null)
    val loadingOlderId: StateFlow<String?> = _loadingOlderId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    fun activeMessages(): List<ChatMessage> {
        val id = _activeId.value ?: return emptyList()
        return _messagesByConversation.value[id] ?: emptyList()
    }

    fun activeHasMore(id: String): Boolean = _pageInfo.value[id]?.hasMore ?: false

    private suspend fun makeClient(): ApiClient = ApiClient(authStore.accessToken())

    // MARK: - Load conversation list

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val client = makeClient()
                _conversations.value = client.listConversations()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load conversations."
            }
        }
    }

    // MARK: - Select / lazy-load messages

    fun selectConversation(id: String) {
        _activeId.value = id
        if (_messagesByConversation.value.containsKey(id)) return

        viewModelScope.launch {
            try {
                val client = makeClient()
                val page = client.getMessages(conversationId = id)
                _messagesByConversation.value = _messagesByConversation.value + (id to page.messages)
                _pageInfo.value = _pageInfo.value + (id to PageInfo(page.nextCursor, page.hasMore))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load messages."
            }
        }
    }

    fun startNewConversation() {
        _activeId.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    // MARK: - Pagination (scroll-up loads older page)

    fun loadOlder(id: String) {
        val info = _pageInfo.value[id] ?: return
        if (!info.hasMore || info.nextCursor == null) return
        if (_loadingOlderId.value == id) return

        viewModelScope.launch {
            _loadingOlderId.value = id
            try {
                val client = makeClient()
                val page = client.getMessages(conversationId = id, before = info.nextCursor)
                val existing = _messagesByConversation.value[id] ?: emptyList()
                _messagesByConversation.value = _messagesByConversation.value + (id to (page.messages + existing))
                _pageInfo.value = _pageInfo.value + (id to PageInfo(page.nextCursor, page.hasMore))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load earlier messages."
            } finally {
                _loadingOlderId.value = null
            }
        }
    }

    // MARK: - Send + stream

    fun send(text: String) {
        viewModelScope.launch {
            val existingId = _activeId.value
            val conversationId = if (existingId != null) {
                existingId
            } else {
                try {
                    val client = makeClient()
                    val conv = client.createConversation()
                    _conversations.value = listOf(conv) + _conversations.value
                    _activeId.value = conv.id
                    conv.id
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to start a new conversation."
                    return@launch
                }
            }

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = Role.user,
                content = text,
                timestamp = Instant.now(),
            )
            append(userMessage, conversationId)

            val assistantMessageId = UUID.randomUUID().toString()
            val assistantMessage = ChatMessage(
                id = assistantMessageId,
                role = Role.assistant,
                content = "",
                timestamp = Instant.now(),
                streaming = true,
            )
            append(assistantMessage, conversationId)

            try {
                val token = authStore.accessToken()
                SseClient.stream(conversationId, text, token)
                    .catch { e -> appendToLast("\n\n[Error: failed to get response]", conversationId) }
                    .collect { event ->
                        when (event) {
                            is SseEvent.Token -> appendToLast(event.text, conversationId)
                            is SseEvent.Title -> updateTitle(event.title, conversationId)
                        }
                    }
            } catch (e: Exception) {
                appendToLast("\n\n[Error: failed to get response]", conversationId)
            }

            setStreaming(false, conversationId)
            loadConversations()
        }
    }

    // MARK: - Local message mutation helpers

    private fun append(message: ChatMessage, conversationId: String) {
        val existing = _messagesByConversation.value[conversationId] ?: emptyList()
        _messagesByConversation.value = _messagesByConversation.value + (conversationId to (existing + message))
    }

    private fun appendToLast(text: String, conversationId: String) {
        val messages = _messagesByConversation.value[conversationId] ?: return
        if (messages.isEmpty()) return
        val updated = messages.toMutableList()
        val last = updated.last()
        updated[updated.lastIndex] = last.copy(content = last.content + text)
        _messagesByConversation.value = _messagesByConversation.value + (conversationId to updated)
    }

    private fun setStreaming(streaming: Boolean, conversationId: String) {
        val messages = _messagesByConversation.value[conversationId] ?: return
        if (messages.isEmpty()) return
        val updated = messages.toMutableList()
        val last = updated.last()
        updated[updated.lastIndex] = last.copy(streaming = streaming)
        _messagesByConversation.value = _messagesByConversation.value + (conversationId to updated)
    }

    private fun updateTitle(title: String, conversationId: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(title = title) else it
        }
    }
}
