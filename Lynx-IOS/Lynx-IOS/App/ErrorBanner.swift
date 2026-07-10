import SwiftUI

/// Lightweight, self-dismissing banner for surfacing `ConversationStore`
/// failures (load/send errors) that previously had no visible UI.
struct ErrorBanner: View {
    let message: String
    var onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 11))
                .foregroundStyle(.red.opacity(0.85))
            Text(message)
                .font(LynxFont.mono(11))
                .foregroundStyle(LynxColor.stone200)
            Spacer(minLength: 8)
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(LynxColor.stone500)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(LynxColor.panelBg)
        .overlay(
            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                .strokeBorder(.red.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
        .padding(.horizontal, 12)
        .transition(.move(edge: .top).combined(with: .opacity))
        .task(id: message) {
            try? await Task.sleep(for: .seconds(4))
            onDismiss()
        }
    }
}
