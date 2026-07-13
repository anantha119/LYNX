package com.anantha.lynx.network

import com.anantha.lynx.model.SseEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure, stateful parser for the backend's SSE stream format. Ported from
 * Lynx-IOS/Lynx-IOS/Networking/SSEParser.swift.
 *
 * Mirrors the parsing done client-side in the web app's `apiSendMessage`
 * (lynx/src/components/ui/chat-app.tsx): frames are `data: <json>\n\n`,
 * buffered text is split on blank lines, and the trailing partial frame is
 * kept for the next chunk. Two JSON `type` values exist: "token" and "title".
 */
class SseParser {
    private val buffer = StringBuilder()
    private val json = Json { ignoreUnknownKeys = true }

    /** Feed a chunk of decoded UTF-8 text; returns any complete events found. */
    fun feed(chunk: String): List<SseEvent> {
        buffer.append(chunk)
        val events = mutableListOf<SseEvent>()

        while (true) {
            val text = buffer.toString()
            val separatorIndex = text.indexOf("\n\n")
            if (separatorIndex < 0) break

            val part = text.substring(0, separatorIndex)
            buffer.delete(0, separatorIndex + 2)

            parseFrame(part)?.let { events.add(it) }
        }

        return events
    }

    private fun parseFrame(part: String): SseEvent? {
        if (!part.startsWith("data: ")) return null
        val jsonString = part.removePrefix("data: ")

        return try {
            val obj = json.parseToJsonElement(jsonString) as? JsonObject ?: return null
            when (obj["type"]?.jsonPrimitive?.content) {
                "token" -> obj["text"]?.jsonPrimitive?.content?.let(SseEvent::Token)
                "title" -> obj["title"]?.jsonPrimitive?.content?.let(SseEvent::Title)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
