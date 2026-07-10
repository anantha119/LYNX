import SwiftUI

/// Main chat surface — mirrors the right-hand pane of chat-app.tsx: a top bar
/// with the active conversation title, either the message thread + input or
/// the hero empty-state + input, and the dark radial-glow background.
struct ChatScreen: View {
    let store: ConversationStore
    let authStore: AuthStore
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

                if let error = store.errorMessage {
                    ErrorBanner(message: error) { store.dismissError() }
                        .padding(.top, 8)
                }

                if store.activeId != nil, !store.activeMessages.isEmpty {
                    MessageThreadView(
                        messages: store.activeMessages,
                        authStore: authStore,
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
            .animation(.easeOut(duration: 0.2), value: store.errorMessage)
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
        .padding(.top, 18)
        .padding(.bottom, 12)
        .overlay(alignment: .bottom) {
            if store.activeId != nil {
                Rectangle().fill(LynxColor.stone900.opacity(0.8)).frame(height: 1)
            }
        }
    }

    private var heroEmptyState: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 10) {
                Text("What can I help")
                    .font(LynxFont.display(28, weight: LynxFont.Display.bold))
                    .foregroundStyle(LynxColor.stone100)
                Text("you ship?")
                    .font(LynxFont.display(28, weight: LynxFont.Display.bold))
                    .foregroundStyle(LynxColor.amberBright)
            }
            .multilineTextAlignment(.center)

            Spacer()

            VStack(spacing: 14) {
                ChatInputBar(text: $draft, onSend: send)
                actionChips
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
        }
        .background(heroGlow)
    }

    private struct ActionChip: Identifiable {
        let id = UUID()
        let icon: String
        let label: String
        let prompt: String
    }

    private static let actionChips: [ActionChip] = [
        ActionChip(icon: "photo.on.rectangle", label: "Clone a Screenshot", prompt: "Clone a screenshot for me"),
        ActionChip(icon: "aspectratio", label: "Import from Figma", prompt: "Import a Figma design"),
        ActionChip(icon: "square.and.arrow.up", label: "Upload a Project", prompt: "Upload and analyze my project"),
        ActionChip(icon: "rectangle.stack", label: "Landing Page", prompt: "Build me a landing page"),
        ActionChip(icon: "person.crop.rectangle", label: "Sign Up Form", prompt: "Build me a sign up form"),
    ]

    private var actionChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Self.actionChips) { chip in
                    Button {
                        sendText(chip.prompt)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: chip.icon)
                                .font(.system(size: 12))
                            Text(chip.label)
                                .font(LynxFont.mono(11))
                        }
                        .foregroundStyle(LynxColor.stone500)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(LynxColor.panelBg)
                        .overlay(
                            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                                .strokeBorder(LynxColor.stone800, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
                    }
                }
            }
        }
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
        sendText(text)
    }

    private func sendText(_ text: String) {
        Task { await store.send(text) }
    }
}
