import Foundation

enum APIError: Error {
    case http(Int)
    case notFound
    case decoding
}

/// REST client for the Lynx backend (see backend/src/index.ts).
/// A native app is not subject to CORS, so this talks to Cloud Run directly.
struct APIClient {
    let accessToken: String

    private var decoder: JSONDecoder { JSONDecoder() }

    private func authorizedRequest(_ path: String, method: String = "GET") -> URLRequest {
        var request = URLRequest(url: AppConfig.apiBaseURL.appending(path: path))
        request.httpMethod = method
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        return request
    }

    func listConversations() async throws -> [Conversation] {
        let (data, response) = try await URLSession.shared.data(for: authorizedRequest("/v1/conversations"))
        try Self.checkStatus(response)
        let decoded = try decoder.decode(ConversationListResponse.self, from: data)
        return decoded.data.map(Conversation.init)
    }

    func createConversation() async throws -> Conversation {
        let (data, response) = try await URLSession.shared.data(
            for: authorizedRequest("/v1/conversations", method: "POST")
        )
        try Self.checkStatus(response)
        let server = try decoder.decode(ServerConversation.self, from: data)
        return Conversation(server: server)
    }

    struct MessagePage {
        let messages: [ChatMessage]
        let nextCursor: String?
        let hasMore: Bool
    }

    func getMessages(conversationId: String, limit: Int = 50, before: String? = nil) async throws -> MessagePage {
        var components = URLComponents(
            url: AppConfig.apiBaseURL.appending(path: "/v1/conversations/\(conversationId)/messages"),
            resolvingAgainstBaseURL: false
        )!
        var query = [URLQueryItem(name: "limit", value: String(limit))]
        if let before { query.append(URLQueryItem(name: "before", value: before)) }
        components.queryItems = query

        var request = URLRequest(url: components.url!)
        request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")

        let (data, response) = try await URLSession.shared.data(for: request)
        try Self.checkStatus(response)
        let decoded = try decoder.decode(MessagesPageResponse.self, from: data)
        return MessagePage(
            messages: decoded.data.map(ChatMessage.init),
            nextCursor: decoded.nextCursor,
            hasMore: decoded.hasMore
        )
    }

    private static func checkStatus(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else { return }
        if http.statusCode == 404 { throw APIError.notFound }
        guard (200..<300).contains(http.statusCode) else { throw APIError.http(http.statusCode) }
    }
}
