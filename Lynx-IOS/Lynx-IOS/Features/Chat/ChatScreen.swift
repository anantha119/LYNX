import SwiftUI

/// Main chat surface — mirrors the right-hand pane of chat-app.tsx: a top bar
/// with the active conversation title, either the message thread + input or
/// the hero empty-state + input, and the dark radial-glow background.
struct ChatScreen: View {
    let store: ConversationStore
    var onOpenSidebar: () -> Void

    @State private var draft = ""

    private var activeConversation: Conversation? {
        store.conversations.first { $0.id == store.activeId }
    }

    var body: some View {
        ZStack {
            LynxColor.bg.ignoresSafeArea()

            VStack(spacing: 0) {
                topBar

                if store.activeId != nil, !store.activeMessages.isEmpty {
                    MessageThreadView(
                        messages: store.activeMessages,
                        hasMore: store.activeId.map(store.activeHasMore) ?? false,
                        loadingOlder: store.loadingOlderId == store.activeId,
                        onLoadOlder: { Task { await store.loadOlder(for: store.activeId!) } }
                    )

                    ChatInputBar(text: $draft, placeholder: "Continue the conversation…", onSend: send)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 10)
                        .background(LynxColor.bg)
                        .overlay(alignment: .top) {
                            Rectangle().fill(LynxColor.stone900.opacity(0.8)).frame(height: 1)
                        }
                } else {
                    heroEmptyState
                }
            }
        }
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            Button(action: onOpenSidebar) {
                Image(systemName: "line.3.horizontal")
                    .foregroundStyle(LynxColor.stone500)
            }

            if let conv = activeConversation {
                Rectangle().fill(LynxColor.amberBright).frame(width: 1, height: 14)
                Text(conv.title)
                    .font(LynxFont.mono(11, weight: LynxFont.Mono.medium))
                    .tracking(1)
                    .foregroundStyle(LynxColor.stone200)
                    .textCase(.uppercase)
                    .lineLimit(1)
            }

            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .overlay(alignment: .bottom) {
            if store.activeId != nil {
                Rectangle().fill(LynxColor.stone900.opacity(0.8)).frame(height: 1)
            }
        }
    }

    private var heroEmptyState: some View {
        VStack {
            Spacer()
            VStack(spacing: 28) {
                VStack(spacing: 10) {
                    Text("What can I help")
                        .font(LynxFont.display(28, weight: LynxFont.Display.bold))
                        .foregroundStyle(LynxColor.stone100)
                    Text("you ship?")
                        .font(LynxFont.display(28, weight: LynxFont.Display.bold))
                        .foregroundStyle(LynxColor.amberBright)
                }
                .multilineTextAlignment(.center)

                ChatInputBar(text: $draft, onSend: send)
            }
            .padding(.horizontal, 20)
            Spacer()
        }
        .background(heroGlow)
    }

    private var heroGlow: some View {
        RadialGradient(
            colors: [LynxColor.amber.opacity(0.08), .clear],
            center: .center,
            startRadius: 0,
            endRadius: 220
        )
        .blur(radius: 60)
        .allowsHitTesting(false)
    }

    private func send() {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        draft = ""
        Task { await store.send(text) }
    }
}
