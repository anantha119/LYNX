package com.anantha.lynx.network

import com.anantha.lynx.model.SseEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    @Test
    fun parsesSingleTokenFrame() {
        val parser = SseParser()
        val events = parser.feed("data: {\"type\":\"token\",\"text\":\"Hello\"}\n\n")

        assertEquals(1, events.size)
        assertEquals(SseEvent.Token("Hello"), events.single())
    }

    @Test
    fun parsesTitleFrame() {
        val parser = SseParser()
        val events = parser.feed("data: {\"type\":\"title\",\"title\":\"New chat\"}\n\n")

        assertEquals(SseEvent.Title("New chat"), events.single())
    }

    @Test
    fun bufferSplitAcrossArbitraryChunkBoundariesStillParses() {
        val parser = SseParser()
        val full = "data: {\"type\":\"token\",\"text\":\"Hi there\"}\n\n"
        val events = mutableListOf<SseEvent>()

        // Feed the frame one character at a time to simulate a stream split
        // at an arbitrary byte boundary.
        for (ch in full) {
            events += parser.feed(ch.toString())
        }

        assertEquals(1, events.size)
        assertEquals(SseEvent.Token("Hi there"), events.single())
    }

    @Test
    fun multipleFramesInOneChunkAllParse() {
        val parser = SseParser()
        val chunk = "data: {\"type\":\"token\",\"text\":\"a\"}\n\n" +
            "data: {\"type\":\"token\",\"text\":\"b\"}\n\n" +
            "data: {\"type\":\"title\",\"title\":\"t\"}\n\n"

        val events = parser.feed(chunk)

        assertEquals(3, events.size)
        assertEquals(SseEvent.Token("a"), events[0])
        assertEquals(SseEvent.Token("b"), events[1])
        assertEquals(SseEvent.Title("t"), events[2])
    }

    @Test
    fun incompleteTrailingFrameIsBufferedUntilCompleted() {
        val parser = SseParser()
        val firstChunkEvents = parser.feed("data: {\"type\":\"token\",\"text\":\"partial\"}\n")
        assertTrue(firstChunkEvents.isEmpty())

        val secondChunkEvents = parser.feed("\n")
        assertEquals(SseEvent.Token("partial"), secondChunkEvents.single())
    }

    @Test
    fun unknownEventTypeIsIgnored() {
        val parser = SseParser()
        val events = parser.feed("data: {\"type\":\"unknown\",\"text\":\"x\"}\n\n")
        assertTrue(events.isEmpty())
    }

    @Test
    fun malformedJsonIsIgnoredWithoutThrowing() {
        val parser = SseParser()
        val events = parser.feed("data: not-json\n\n")
        assertTrue(events.isEmpty())
    }

    @Test
    fun nonDataPrefixedFrameIsIgnored() {
        val parser = SseParser()
        val events = parser.feed(": heartbeat\n\n")
        assertTrue(events.isEmpty())
    }
}
