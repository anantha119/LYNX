import SwiftUI

/// Mobile reinterpretation of the web sidebar (chat-sidebar.tsx): a dedicated
/// screen reachable via a swipe-in drawer or push, grouped by recency.
struct ConversationListView: View {
    let store: ConversationStore
    let authStore: AuthStore
    var onSelect: (String) -> Void
    var onNew: () -> Void

    var body: some View {
        ZStack {
            LynxColor.sidebarBg.ignoresSafeArea()

            VStack(spacing: 0) {
                header
                newConversationButton

                if groups.isEmpty {
                    emptyState
                } else {
                    List {
                        ForEach(groups, id: \.label) { group in
                            Section {
                                ForEach(group.items) { conv in
                                    ConversationRow(conversation: conv, isActive: conv.id == store.activeId)
                                        .listRowBackground(Color.clear)
                                        .listRowSeparator(.hidden)
                                        .contentShape(Rectangle())
                                        .onTapGesture { onSelect(conv.id) }
                                }
                            } header: {
                                Text(group.label)
                                    .font(LynxFont.mono(9, weight: LynxFont.Mono.medium))
                                    .tracking(2)
                                    .foregroundStyle(LynxColor.stone700)
                                    .textCase(.uppercase)
                            }
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }

                Spacer(minLength: 0)
                userFooter
            }
        }
        .task { await store.loadConversations() }
    }

    private var header: some View {
        HStack(spacing: 10) {
            LynxMark(size: 20)
            Text("LYNX")
                .font(LynxFont.display(15, weight: LynxFont.Display.bold))
                .tracking(2)
                .foregroundStyle(LynxColor.stone100)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
    }

    private var newConversationButton: some View {
        Button(action: onNew) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 12, weight: .semibold))
                Text("New conversation")
                    .font(LynxFont.mono(12))
            }
            .foregroundStyle(LynxColor.stone500)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius)
                    .strokeBorder(LynxColor.stone800, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            )
        }
        .padding(.horizontal, 12)
        .padding(.bottom, 12)
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Spacer()
            Image(systemName: "message")
                .font(.system(size: 22))
                .foregroundStyle(LynxColor.stone800)
            Text("No conversations yet.\nStart a new one above.")
                .font(LynxFont.mono(10))
                .multilineTextAlignment(.center)
                .foregroundStyle(LynxColor.stone700)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private var userFooter: some View {
        HStack(spacing: 10) {
            AvatarView(authStore: authStore, size: 26)
            VStack(alignment: .leading, spacing: 1) {
                Text(displayName)
                    .font(LynxFont.mono(12))
                    .foregroundStyle(LynxColor.stone300)
                    .lineLimit(1)
                Text("Free plan")
                    .font(LynxFont.mono(9))
                    .foregroundStyle(LynxColor.stone700)
            }
            Spacer()
            Button {
                Task { await authStore.logout() }
            } label: {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .foregroundStyle(LynxColor.stone700)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .overlay(alignment: .top) {
            Rectangle().fill(LynxColor.stone900).frame(height: 1)
        }
    }

    private var displayName: String {
        if case .signedIn(let user) = authStore.state { return user.name }
        return "User"
    }

    // MARK: - Time grouping (mirrors chat-sidebar.tsx groupConversations)

    private struct Group {
        let label: String
        let items: [Conversation]
    }

    private var groups: [Group] {
        let now = Date()
        var today: [Conversation] = [], yesterday: [Conversation] = []
        var week: [Conversation] = [], older: [Conversation] = []

        for conv in store.conversations {
            let days = now.timeIntervalSince(conv.updatedAt) / 86400
            if days < 1 { today.append(conv) }
            else if days < 2 { yesterday.append(conv) }
            else if days < 7 { week.append(conv) }
            else { older.append(conv) }
        }

        return [
            Group(label: "Today", items: today),
            Group(label: "Yesterday", items: yesterday),
            Group(label: "Past 7 days", items: week),
            Group(label: "Older", items: older),
        ].filter { !$0.items.isEmpty }
    }
}

private struct ConversationRow: View {
    let conversation: Conversation
    let isActive: Bool

    var body: some View {
        HStack(spacing: 8) {
            Rectangle()
                .fill(isActive ? LynxColor.amberBright : .clear)
                .frame(width: 2)

            Text(conversation.title)
                .font(LynxFont.mono(13))
                .foregroundStyle(isActive ? LynxColor.stone100 : LynxColor.stone400)
                .lineLimit(1)

            Spacer()

            if isActive {
                Image(systemName: "chevron.right")
                    .font(.system(size: 10))
                    .foregroundStyle(LynxColor.amberBright)
            }
        }
        .padding(.vertical, 8)
        .padding(.trailing, 8)
        .background(isActive ? LynxColor.stone900.opacity(0.8) : .clear)
        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius))
    }
}

/// User avatar — mirrors the sidebar's amber-tinted initial/photo circle.
struct AvatarView: View {
    let authStore: AuthStore
    var size: CGFloat = 24

    var body: some View {
        Group {
            if case .signedIn(let user) = authStore.state {
                if let url = user.pictureURL {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        initialBadge(for: user.name)
                    }
                } else {
                    initialBadge(for: user.name)
                }
            } else {
                initialBadge(for: "U")
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius * 0.7))
    }

    private func initialBadge(for name: String) -> some View {
        ZStack {
            LynxColor.amber.opacity(0.2)
            Text(String(name.prefix(1)).uppercased())
                .font(LynxFont.mono(size * 0.42, weight: LynxFont.Mono.medium))
                .foregroundStyle(LynxColor.amberBright)
        }
        .overlay(
            RoundedRectangle(cornerRadius: LynxMetrics.cornerRadius * 0.7)
                .strokeBorder(LynxColor.amber.opacity(0.3), lineWidth: 1)
        )
    }
}
