import SwiftUI

/// Mirrors the message row in chat-messages.tsx: user bubbles right-aligned
/// in a stone box, assistant messages left-aligned with an amber rule.
struct MessageBubble: View {
    let message: ChatMessage
    let authStore: AuthStore

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            if message.role == .assistant {
                LynxMark(size: 18).padding(.top, 2)
                content
                Spacer(minLength: 24)
            } else {
                Spacer(minLength: 24)
                content
                AvatarView(authStore: authStore, size: 22)
                    .padding(.top, 2)
            }
        }
    }

    private var content: some View {
        Group {
            if message.role == .user {
                // Relative to the scroll container's width (mirrors the web's
                // max-w-[72%]) so it scales across iPhone SE through Pro Max,
                // instead of a fixed point width.
                bubbleStack
                    .containerRelativeFrame(.horizontal, alignment: .trailing) { width, _ in width * 0.72 }
            } else {
                bubbleStack
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var bubbleStack: some View {
        VStack(alignment: message.role == .user ? .trailing : .leading, spacing: 4) {
            Group {
                if message.role == .assistant {
                    MarkdownMessage(content: message.content, streaming: message.streaming)
                        .padding(.leading, 12)
                        .overlay(alignment: .leading) {
                            Rectangle().fill(LynxColor.amber.opacity(0.6)).frame(width: 2)
                        }
                } else {
                    Text(message.content)
                        .font(LynxFont.mono(14))
                        .foregroundStyle(LynxColor.stone100)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(LynxColor.stone800)
                        .overlay(
                            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                                .strokeBorder(LynxColor.stone700, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
                }
            }

            Text(Self.timeFormatter.string(from: message.timestamp))
                .font(LynxFont.mono(9))
                .foregroundStyle(LynxColor.stone700)
                .padding(.horizontal, 4)
        }
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.timeStyle = .short
        return f
    }()
}
