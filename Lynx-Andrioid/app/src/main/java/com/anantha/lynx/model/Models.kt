package com.anantha.lynx.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// MARK: - Wire types (exact backend JSON shapes)
// See backend/src/index.ts, backend/src/db/conversations.ts, backend/src/db/messages.ts
// Ported 1:1 from Lynx-IOS/Lynx-IOS/Models/Models.swift.

@Serializable
enum class Role {
    @SerialName("user") user,
    @SerialName("assistant") assistant,
    @SerialName("system") system,
    @SerialName("tool") tool,
}

@Serializable
enum class MessageStatus {
    @SerialName("complete") complete,
    @SerialName("streaming") streaming,
    @SerialName("error") error,
}

@Serializable
data class MessagePart(
    val type: String,
    val text: String? = null,
)

@Serializable
data class ServerConversation(
    val id: String,
    val title: String? = null,
    val model: String,
    @SerialName("message_count") val messageCount: Int,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ServerMessage(
    val id: String,
    val role: Role,
    val content: List<MessagePart>,
    val status: MessageStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("token_count") val tokenCount: Int? = null,
)

@Serializable
data class ConversationListResponse(
    val data: List<ServerConversation>,
)

@Serializable
data class MessagesPageResponse(
    val data: List<ServerMessage>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class SendMessageBody(
    val content: String,
)

// MARK: - SSE event payloads

sealed class SseEvent {
    data class Token(val text: String) : SseEvent()
    data class Title(val title: String) : SseEvent()
}

// MARK: - UI-facing models (mirrors chat-app.tsx's Conversation / Message mapping)

data class Conversation(
    val id: String,
    var title: String,
    var updatedAt: Instant,
) {
    constructor(server: ServerConversation) : this(
        id = server.id,
        title = server.title ?: "New conversation",
        updatedAt = lynxParseInstant(server.lastMessageAt ?: server.createdAt) ?: Instant.now(),
    )
}

data class ChatMessage(
    val id: String,
    val role: Role,
    var content: String,
    val timestamp: Instant,
    var streaming: Boolean = false,
) {
    constructor(server: ServerMessage) : this(
        id = server.id,
        role = server.role,
        content = server.content.mapNotNull { it.text }.joinToString(""),
        timestamp = lynxParseInstant(server.createdAt) ?: Instant.now(),
        streaming = server.status == MessageStatus.streaming,
    )
}

/** Postgres TIMESTAMPTZ comes back with or without fractional seconds. */
private fun lynxParseInstant(s: String): Instant? = try {
    Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s))
} catch (e: DateTimeParseException) {
    null
}
