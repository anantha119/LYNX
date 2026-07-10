//
//  Lynx_IOSTests.swift
//  Lynx-IOSTests
//
//  Created by Anantha Padmanaban Krishna Kumar on 7/10/26.
//

import Testing
@testable import Lynx_IOS

struct SSEParserTests {

    @Test func parsesTokenEvent() {
        var parser = SSEParser()
        let events = parser.feed("data: {\"type\":\"token\",\"text\":\"Hello\"}\n\n")
        #expect(events == [.token("Hello")])
    }

    @Test func parsesTitleEvent() {
        var parser = SSEParser()
        let events = parser.feed("data: {\"type\":\"title\",\"title\":\"New chat\"}\n\n")
        #expect(events == [.title("New chat")])
    }

    @Test func bufferSplitAcrossChunks() {
        var parser = SSEParser()
        // Frame arrives split mid-JSON across two network reads.
        let first = parser.feed("data: {\"type\":\"token\",\"tex")
        #expect(first.isEmpty)
        let second = parser.feed("t\":\"lo\"}\n\n")
        #expect(second == [.token("lo")])
    }

    @Test func multipleFramesInOneChunk() {
        var parser = SSEParser()
        let events = parser.feed(
            "data: {\"type\":\"token\",\"text\":\"a\"}\n\ndata: {\"type\":\"token\",\"text\":\"b\"}\n\n"
        )
        #expect(events == [.token("a"), .token("b")])
    }

    @Test func ignoresUnknownTypeAndKeepsTrailingPartial() {
        var parser = SSEParser()
        let events = parser.feed("data: {\"type\":\"unknown\"}\n\ndata: {\"type\":\"token\"")
        #expect(events.isEmpty)
        let rest = parser.feed(",\"text\":\"x\"}\n\n")
        #expect(rest == [.token("x")])
    }
}
