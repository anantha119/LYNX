import Foundation

enum SSEError: Error {
    case httpError(Int)
}

/// Streams the SSE response body of POST /v1/conversations/:id/messages
/// and yields decoded `SSEEvent`s as they arrive.
enum SSEClient {
    static func stream(
        conversationId: String,
        content: String,
        accessToken: String
    ) -> AsyncThrowingStream<SSEEvent, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    var request = URLRequest(
                        url: AppConfig.apiBaseURL.appending(path: "/v1/conversations/\(conversationId)/messages")
                    )
                    request.httpMethod = "POST"
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
                    request.httpBody = try JSONEncoder().encode(SendMessageBody(content: content))

                    let (bytes, response) = try await URLSession.shared.bytes(for: request)
                    if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                        throw SSEError.httpError(http.statusCode)
                    }

                    var parser = SSEParser()
                    var lineBuffer = Data()
                    for try await byte in bytes {
                        lineBuffer.append(byte)
                        // Decode eagerly in UTF-8-safe chunks by flushing on newlines,
                        // matching the web client's TextDecoder(stream:true) behavior.
                        if byte == UInt8(ascii: "\n"), let chunk = String(data: lineBuffer, encoding: .utf8) {
                            for event in parser.feed(chunk) {
                                continuation.yield(event)
                            }
                            lineBuffer.removeAll(keepingCapacity: true)
                        }
                    }
                    if !lineBuffer.isEmpty, let chunk = String(data: lineBuffer, encoding: .utf8) {
                        for event in parser.feed(chunk) {
                            continuation.yield(event)
                        }
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
