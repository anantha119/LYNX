import Auth0
import SwiftUI

/// Auth-gated root: LoginView until signed in, then the drawer + chat surface.
/// Mirrors the web app's top-level `ChatApp` (chat-app.tsx) — sidebar + main
/// pane — reinterpreted as a slide-in drawer for mobile.
struct RootView: View {
    @State private var authStore = AuthStore()
    @State private var conversationStore: ConversationStore?

    var body: some View {
        Group {
            switch authStore.state {
            case .loading:
                loadingView
            case .signedOut:
                LoginView(authStore: authStore)
            case .signedIn:
                if let conversationStore {
                    MainAppView(store: conversationStore, authStore: authStore)
                } else {
                    loadingView
                        .task { conversationStore = ConversationStore(authStore: authStore) }
                }
            }
        }
        .onOpenURL { url in
            WebAuthentication.resume(with: url)
        }
        .preferredColorScheme(.dark)
    }

    private var loadingView: some View {
        ZStack {
            LynxColor.bg.ignoresSafeArea()
            ProgressView().tint(LynxColor.amberBright)
        }
    }
}

/// Signed-in shell: chat surface with a swipe-in conversation drawer.
private struct MainAppView: View {
    let store: ConversationStore
    let authStore: AuthStore

    @State private var isSidebarOpen = false
    @GestureState private var dragOffset: CGFloat = 0

    private let sidebarWidth: CGFloat = 280

    var body: some View {
        ZStack(alignment: .leading) {
            ChatScreen(
                store: store,
                authStore: authStore,
                onOpenSidebar: { withAnimation(.easeOut(duration: 0.25)) { isSidebarOpen = true } }
            )

            if isSidebarOpen {
                Color.black.opacity(0.6)
                    .ignoresSafeArea()
                    .onTapGesture { withAnimation(.easeOut(duration: 0.25)) { isSidebarOpen = false } }
                    .transition(.opacity)
            }

            ConversationListView(
                store: store,
                authStore: authStore,
                onSelect: { id in
                    Task { await store.selectConversation(id) }
                    withAnimation(.easeOut(duration: 0.25)) { isSidebarOpen = false }
                },
                onNew: {
                    store.startNewConversation()
                    withAnimation(.easeOut(duration: 0.25)) { isSidebarOpen = false }
                }
            )
            .frame(width: sidebarWidth)
            .offset(x: isSidebarOpen ? dragOffset.clamped(to: ...0) : -sidebarWidth)
            .animation(.easeOut(duration: 0.25), value: isSidebarOpen)
            .gesture(
                DragGesture()
                    .updating($dragOffset) { value, state, _ in
                        state = value.translation.width
                    }
                    .onEnded { value in
                        if value.translation.width < -sidebarWidth * 0.3 {
                            isSidebarOpen = false
                        }
                    }
            )
        }
    }
}

private extension Comparable {
    func clamped(to range: PartialRangeThrough<Self>) -> Self {
        min(self, range.upperBound)
    }
}
