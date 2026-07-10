import Foundation

/// Pure, stateful parser for the backend's SSE stream format.
///
/// Mirrors the parsing done client-side in the web app's `apiSendMessage`
/// (lynx/src/components/ui/chat-app.tsx): frames are `data: <json>\n\n`,
/// buffered text is split on blank lines, and the trailing partial frame is
/// kept for the next chunk. Two JSON `type` values exist: "token" and "title".
struct SSEParser {
    private var buffer = ""

    /// Feed a chunk of decoded UTF-8 text; returns any complete events found.
    mutating func feed(_ chunk: String) -> [SSEEvent] {
        buffer += chunk
        var events: [SSEEvent] = []
        while let range = buffer.range(of: "\n\n") {
            let part = String(buffer[buffer.startIndex..<range.lowerBound])
            buffer.removeSubrange(buffer.startIndex..<range.upperBound)
            if let event = Self.parseFrame(part) {
                events.append(event)
            }
        }
        return events
    }

    static func parseFrame(_ part: String) -> SSEEvent? {
        guard part.hasPrefix("data: ") else { return nil }
        let jsonString = String(part.dropFirst("data: ".count))
        guard
            let data = jsonString.data(using: .utf8),
            let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let type = obj["type"] as? String
        else { return nil }

        switch type {
        case "token":
            guard let text = obj["text"] as? String else { return nil }
            return .token(text)
        case "title":
            guard let title = obj["title"] as? String else { return nil }
            return .title(title)
        default:
            return nil
        }
    }
}
