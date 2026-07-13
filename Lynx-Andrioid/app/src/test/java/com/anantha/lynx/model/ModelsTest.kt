package com.anantha.lynx.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesConversationListResponse() {
        val body = """
            {"data":[{"id":"01H","title":"Hello","model":"gemini-3.1-flash-lite","message_count":2,"last_message_at":"2026-07-10T12:00:00.123Z","created_at":"2026-07-10T11:00:00Z"}]}
        """.trimIndent()

        val decoded = json.decodeFromString<ConversationListResponse>(body)
        val conversation = Conversation(decoded.data.single())

        assertEquals("01H", conversation.id)
        assertEquals("Hello", conversation.title)
    }

    @Test
    fun conversationFallsBackToPlaceholderTitle() {
        val server = ServerConversation(
            id = "01H",
            title = null,
            model = "gemini-3.1-flash-lite",
            messageCount = 0,
            lastMessageAt = null,
            createdAt = "2026-07-10T11:00:00Z",
        )
        assertEquals("New conversation", Conversation(server).title)
    }

    @Test
    fun decodesMessagesPageResponseWithCursor() {
        val body = """
            {"data":[{"id":"01M","role":"assistant","content":[{"type":"text","text":"Hi"}],"status":"complete","created_at":"2026-07-10T11:00:00Z","token_count":3}],"next_cursor":"01M","has_more":true}
        """.trimIndent()

        val decoded = json.decodeFromString<MessagesPageResponse>(body)
        val message = ChatMessage(decoded.data.single())

        assertEquals(Role.assistant, message.role)
        assertEquals("Hi", message.content)
        assertEquals(false, message.streaming)
        assertTrue(decoded.hasMore)
        assertEquals("01M", decoded.nextCursor)
    }

    @Test
    fun streamingStatusMapsToStreamingFlag() {
        val server = ServerMessage(
            id = "01M",
            role = Role.assistant,
            content = listOf(MessagePart(type = "text", text = "partial")),
            status = MessageStatus.streaming,
            createdAt = "2026-07-10T11:00:00Z",
            tokenCount = null,
        )
        assertTrue(ChatMessage(server).streaming)
    }

    @Test
    fun sseTokenAndTitleEventsDecodeAsExpectedTypes() {
        val token = SseEvent.Token("hello")
        val title = SseEvent.Title("New Title")
        assertEquals("hello", token.text)
        assertEquals("New Title", title.title)
    }
}
