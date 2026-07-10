import SwiftUI

/// Mobile version of the "terminal chrome" input from v0-ai-chat.tsx:
/// amber corner marks, auto-growing text field, amber send button.
struct ChatInputBar: View {
    @Binding var text: String
    var placeholder: String = "Describe what you want to build…"
    var onSend: () -> Void

    @FocusState private var isFocused: Bool

    private var canSend: Bool { !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    var body: some View {
        VStack(spacing: 0) {
            TextField("", text: $text, prompt: Text(placeholder).foregroundStyle(LynxColor.stone600), axis: .vertical)
                .font(LynxFont.mono(14))
                .foregroundStyle(LynxColor.stone200)
                .tint(LynxColor.amberBright)
                .lineLimit(1...6)
                .focused($isFocused)
                .padding(.horizontal, 14)
                .padding(.vertical, 14)
                .onSubmit(send)

            HStack {
                Button {} label: {
                    Image(systemName: "paperclip")
                        .foregroundStyle(LynxColor.stone600)
                }
                Spacer()
                Button(action: send) {
                    Image(systemName: "arrow.up")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(canSend ? .black : LynxColor.stone600)
                        .frame(width: 28, height: 28)
                        .background(canSend ? LynxColor.amberBright : .clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                                .strokeBorder(canSend ? .clear : LynxColor.stone700, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
                }
                .disabled(!canSend)
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 10)
            .padding(.top, 4)
            .overlay(alignment: .top) {
                Rectangle().fill(LynxColor.stone800.opacity(0.8)).frame(height: 1)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                .fill(LynxColor.panelBg)
                .overlay(
                    RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                        .strokeBorder(LynxColor.stone800, lineWidth: 1)
                )
        )
        // .overlay proposes the base view's own (now content-sized) frame to
        // the GeometryReader below, instead of a ZStack sibling's unbounded one.
        .overlay(cornerMarks)
        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
    }

    private func send() {
        guard canSend else { return }
        onSend()
    }

    private var cornerMarks: some View {
        GeometryReader { geo in
            let mark: CGFloat = 10
            ForEach(0..<4, id: \.self) { corner in
                Path { path in
                    let isTop = corner < 2
                    let isLeft = corner % 2 == 0
                    let x: CGFloat = isLeft ? 0 : geo.size.width
                    let y: CGFloat = isTop ? 0 : geo.size.height
                    let dx: CGFloat = isLeft ? mark : -mark
                    let dy: CGFloat = isTop ? mark : -mark
                    path.move(to: CGPoint(x: x, y: y + dy))
                    path.addLine(to: CGPoint(x: x, y: y))
                    path.addLine(to: CGPoint(x: x + dx, y: y))
                }
                .stroke(LynxColor.amber.opacity(0.6), lineWidth: 1)
            }
        }
        .allowsHitTesting(false)
    }
}
