import Foundation
import Observation

/// Mirrors the state machine in the web app's `ChatApp` (chat-app.tsx):
/// token → conversation list → lazy-loaded messages per conversation →
/// optimistic send + SSE token streaming → refresh list for title/reorder.
@Observable
@MainActor
final class ConversationStore {
    private(set) var conversations: [Conversation] = []
    private(set) var messagesByConversation: [String: [ChatMessage]] = [:]
    private(set) var pageInfo: [String: (nextCursor: String?, hasMore: Bool)] = [:]
    private(set) var loadingOlderId: String?
    private(set) var errorMessage: String?

    var activeId: String?

    private let authStore: AuthStore

    init(authStore: AuthStore) {
        self.authStore = authStore
    }

    var activeMessages: [ChatMessage] {
        guard let activeId else { return [] }
        return messagesByConversation[activeId] ?? []
    }

    func activeHasMore(_ id: String) -> Bool {
        pageInfo[id]?.hasMore ?? false
    }

    private func makeClient() async throws -> APIClient {
        APIClient(accessToken: try await authStore.accessToken())
    }

    // MARK: - Load conversation list

    func loadConversations() async {
        do {
            let client = try await makeClient()
            conversations = try await client.listConversations()
            errorMessage = nil
        } catch {
            errorMessage = "Failed to load conversations."
        }
    }

    // MARK: - Select / lazy-load messages

    func selectConversation(_ id: String) async {
        activeId = id
        guard messagesByConversation[id] == nil else { return }
        do {
            let client = try await makeClient()
            let page = try await client.getMessages(conversationId: id)
            messagesByConversation[id] = page.messages
            pageInfo[id] = (page.nextCursor, page.hasMore)
        } catch {
            errorMessage = "Failed to load messages."
        }
    }

    func startNewConversation() {
        activeId = nil
    }

    // MARK: - Pagination (scroll-up loads older page)

    func loadOlder(for id: String) async {
        guard let info = pageInfo[id], info.hasMore, let cursor = info.nextCursor else { return }
        guard loadingOlderId != id else { return }
        loadingOlderId = id
        defer { loadingOlderId = nil }
        do {
            let client = try await makeClient()
            let page = try await client.getMessages(conversationId: id, before: cursor)
            let existing = messagesByConversation[id] ?? []
            messagesByConversation[id] = page.messages + existing
            pageInfo[id] = (page.nextCursor, page.hasMore)
        } catch {
            errorMessage = "Failed to load earlier messages."
        }
    }

    // MARK: - Send + stream

    func send(_ text: String) async {
        var convId = activeId
        if convId == nil {
            do {
                let client = try await makeClient()
                let conv = try await client.createConversation()
                convId = conv.id
                conversations.insert(conv, at: 0)
                activeId = conv.id
            } catch {
                errorMessage = "Failed to start a new conversation."
                return
            }
        }
        guard let convId else { return }

        let userMsg = ChatMessage(id: UUID().uuidString, role: .user, content: text, timestamp: Date())
        append(userMsg, to: convId)

        let aiMsgId = UUID().uuidString
        let aiMsg = ChatMessage(id: aiMsgId, role: .assistant, content: "", timestamp: Date(), streaming: true)
        append(aiMsg, to: convId)

        do {
            let token = try await authStore.accessToken()
            let stream = SSEClient.stream(conversationId: convId, content: text, accessToken: token)
            for try await event in stream {
                switch event {
                case .token(let chunk):
                    appendToLast(chunk, in: convId)
                case .title(let title):
                    updateTitle(title, for: convId)
                }
            }
        } catch {
            appendToLast("\n\n[Error: failed to get response]", in: convId)
        }

        setStreaming(false, in: convId)
        await loadConversations()
    }

    // MARK: - Local message mutation helpers

    private func append(_ message: ChatMessage, to conversationId: String) {
        messagesByConversation[conversationId, default: []].append(message)
    }

    private func appendToLast(_ text: String, in conversationId: String) {
        guard var messages = messagesByConversation[conversationId], !messages.isEmpty else { return }
        let lastIndex = messages.count - 1
        messages[lastIndex].content += text
        messagesByConversation[conversationId] = messages
    }

    private func setStreaming(_ streaming: Bool, in conversationId: String) {
        guard var messages = messagesByConversation[conversationId], !messages.isEmpty else { return }
        let lastIndex = messages.count - 1
        messages[lastIndex].streaming = streaming
        messagesByConversation[conversationId] = messages
    }

    private func updateTitle(_ title: String, for conversationId: String) {
        guard let index = conversations.firstIndex(where: { $0.id == conversationId }) else { return }
        conversations[index].title = title
    }
}
