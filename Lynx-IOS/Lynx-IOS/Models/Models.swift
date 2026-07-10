import Foundation

// MARK: - Wire types (exact backend JSON shapes)
// See backend/src/index.ts, backend/src/db/conversations.ts, backend/src/db/messages.ts

enum Role: String, Codable {
    case user, assistant, system, tool
}

enum MessageStatus: String, Codable {
    case complete, streaming, error
}

struct MessagePart: Codable, Hashable {
    let type: String
    let text: String?
}

struct ServerConversation: Codable {
    let id: String
    let title: String?
    let model: String
    let messageCount: Int
    let lastMessageAt: String?
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id, title, model
        case messageCount = "message_count"
        case lastMessageAt = "last_message_at"
        case createdAt = "created_at"
    }
}

struct ServerMessage: Codable {
    let id: String
    let role: Role
    let content: [MessagePart]
    let status: MessageStatus
    let createdAt: String
    let tokenCount: Int?

    enum CodingKeys: String, CodingKey {
        case id, role, content, status
        case createdAt = "created_at"
        case tokenCount = "token_count"
    }
}

struct ConversationListResponse: Codable {
    let data: [ServerConversation]
}

struct MessagesPageResponse: Codable {
    let data: [ServerMessage]
    let nextCursor: String?
    let hasMore: Bool

    enum CodingKeys: String, CodingKey {
        case data
        case nextCursor = "next_cursor"
        case hasMore = "has_more"
    }
}

struct SendMessageBody: Codable {
    let content: String
}

// MARK: - SSE event payloads

enum SSEEvent: Equatable {
    case token(String)
    case title(String)
}

// MARK: - UI-facing models (mirrors chat-app.tsx's Conversation / Message mapping)

struct Conversation: Identifiable, Equatable {
    let id: String
    var title: String
    var updatedAt: Date

    init(server: ServerConversation) {
        id = server.id
        title = server.title ?? "New conversation"
        updatedAt = ISO8601DateFormatter.lynxParse(server.lastMessageAt ?? server.createdAt) ?? Date()
    }
}

struct ChatMessage: Identifiable, Equatable {
    let id: String
    let role: Role
    var content: String
    let timestamp: Date
    var streaming: Bool = false

    init(server: ServerMessage) {
        id = server.id
        role = server.role
        content = server.content.compactMap(\.text).joined()
        timestamp = ISO8601DateFormatter.lynxParse(server.createdAt) ?? Date()
        streaming = server.status == .streaming
    }

    init(id: String, role: Role, content: String, timestamp: Date, streaming: Bool = false) {
        self.id = id
        self.role = role
        self.content = content
        self.timestamp = timestamp
        self.streaming = streaming
    }
}

extension ISO8601DateFormatter {
    private static let withFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    private static let plain = ISO8601DateFormatter()

    /// Postgres TIMESTAMPTZ comes back with or without fractional seconds.
    static func lynxParse(_ s: String) -> Date? {
        withFractional.date(from: s) ?? plain.date(from: s)
    }
}
