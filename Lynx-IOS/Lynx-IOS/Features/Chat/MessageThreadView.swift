import SwiftUI

/// Scrollback thread with auto-scroll-to-bottom on new tokens and a
/// scroll-to-top trigger for loading older pages — mirrors the anchoring
/// logic in chat-messages.tsx.
struct MessageThreadView: View {
    let messages: [ChatMessage]
    let hasMore: Bool
    let loadingOlder: Bool
    let onLoadOlder: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 20) {
                    if hasMore {
                        Color.clear
                            .frame(height: 1)
                            .onAppear(perform: onLoadOlder)
                    }
                    if loadingOlder {
                        Text("Loading earlier messages…")
                            .font(LynxFont.mono(10))
                            .foregroundStyle(LynxColor.stone600)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                    }

                    ForEach(messages) { message in
                        MessageBubble(message: message)
                            .id(message.id)
                    }

                    Color.clear.frame(height: 1).id("bottom")
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .onChange(of: messages.count) {
                withAnimation(.easeOut(duration: 0.2)) {
                    proxy.scrollTo("bottom", anchor: .bottom)
                }
            }
            .onChange(of: messages.last?.content) {
                proxy.scrollTo("bottom", anchor: .bottom)
            }
            .onAppear {
                proxy.scrollTo("bottom", anchor: .bottom)
            }
        }
    }
}
