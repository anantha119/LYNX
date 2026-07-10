import SwiftUI

struct LoginView: View {
    let authStore: AuthStore
    @State private var isSigningIn = false

    var body: some View {
        ZStack {
            LynxColor.bg.ignoresSafeArea()

            // Radial dot-grid + amber glow, matching the web hero background.
            Canvas { context, size in
                let spacing: CGFloat = 28
                var x: CGFloat = 0
                while x < size.width {
                    var y: CGFloat = 0
                    while y < size.height {
                        context.fill(
                            Path(ellipseIn: CGRect(x: x, y: y, width: 1.4, height: 1.4)),
                            with: .color(.white.opacity(0.045))
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
            .ignoresSafeArea()

            RadialGradient(
                colors: [LynxColor.amber.opacity(0.10), .clear],
                center: .center,
                startRadius: 0,
                endRadius: 260
            )
            .blur(radius: 60)
            .ignoresSafeArea()

            VStack(spacing: 36) {
                Spacer()

                VStack(spacing: 14) {
                    LynxMark(size: 40)

                    Text("LYNX")
                        .font(LynxFont.display(22, weight: LynxFont.Display.bold))
                        .tracking(4)
                        .foregroundStyle(LynxColor.stone100)

                    Text("What can I help\nyou ship?")
                        .font(LynxFont.display(30, weight: LynxFont.Display.bold))
                        .multilineTextAlignment(.center)
                        .foregroundStyle(LynxColor.stone100)
                }

                if let error = authStore.lastError {
                    Text(error)
                        .font(LynxFont.mono(11))
                        .foregroundStyle(.red.opacity(0.85))
                }

                Spacer()

                Button {
                    Task {
                        isSigningIn = true
                        await authStore.login()
                        isSigningIn = false
                    }
                } label: {
                    HStack(spacing: 8) {
                        if isSigningIn {
                            ProgressView()
                                .tint(.black)
                        } else {
                            Image(systemName: "arrow.right.circle.fill")
                        }
                        Text(isSigningIn ? "Signing in…" : "Continue with Auth0")
                            .font(LynxFont.mono(13, weight: LynxFont.Mono.medium))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(LynxColor.amberBright)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
                }
                .disabled(isSigningIn)
                .padding(.horizontal, 32)
                .padding(.bottom, 48)
            }
        }
    }
}

/// The amber star mark used throughout the web app (sidebar logo, assistant avatar).
struct LynxMark: View {
    var size: CGFloat = 24

    var body: some View {
        Image(systemName: "sparkle")
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(LynxColor.amber)
    }
}
