import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var store: AppStore
    @State private var selected: WordEntry? = nil
    @State private var showConfirmClear = false

    var body: some View {
        NavigationStack {
            Group {
                if store.history.isEmpty {
                    EmptyState(
                        icon: "clock",
                        title: String(localized: "history.emptyTitle"),
                        subtitle: String(localized: "history.emptySubtitle")
                    )
                } else {
                    List {
                        Section {
                            ForEach(store.history) { entry in
                                Button { selected = entry } label: {
                                    EntryRow(entry: entry)
                                }
                                .listRowBackground(Color(.systemBackground))
                            }
                            .onDelete(perform: store.deleteHistory)
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle(String(localized: "history.navTitle"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    if !store.history.isEmpty {
                        Button(role: .destructive) { showConfirmClear = true } label: {
                            Text(String(localized: "history.clearButton"))
                        }
                    }
                }
            }
            .confirmationDialog(String(localized: "history.clearConfirm"), isPresented: $showConfirmClear, titleVisibility: .visible) {
                Button(String(localized: "history.clearButton"), role: .destructive) { store.clearHistory() }
                Button(String(localized: "history.cancelButton"), role: .cancel) {}
            }
            .sheet(item: $selected) { entry in
                EntryDetailView(entry: entry)
            }
        }
    }
}

// MARK: - Favorites View
struct FavoritesView: View {
    @EnvironmentObject var store: AppStore
    @State private var selected: WordEntry? = nil

    var body: some View {
        NavigationStack {
            Group {
                if store.favorites.isEmpty {
                    EmptyState(
                        icon: "star",
                        title: String(localized: "favorites.emptyTitle"),
                        subtitle: String(localized: "favorites.emptySubtitle")
                    )
                } else {
                    List {
                        Section {
                            ForEach(store.favorites) { entry in
                                Button { selected = entry } label: {
                                    EntryRow(entry: entry, showStar: true)
                                }
                                .listRowBackground(Color(.systemBackground))
                            }
                            .onDelete(perform: store.deleteFavorite)
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle(String(localized: "favorites.navTitle"))
            .sheet(item: $selected) { entry in
                EntryDetailView(entry: entry)
            }
        }
    }
}

// MARK: - Row
struct EntryRow: View {
    let entry: WordEntry
    var showStar: Bool = false

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: showStar ? "star.fill" : entry.cached ? "memorychip" : "magnifyingglass")
                .foregroundColor(showStar ? Color.accentOrange : .secondary)
                .font(.system(size: 18))
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 3) {
                Text(entry.query)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.primary)
                Text("\(entry.relativeTime) · \(entry.modelShort)")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Entry Detail
struct EntryDetailView: View {
    @EnvironmentObject var store: AppStore
    let entry: WordEntry
    @Environment(\.dismiss) var dismiss
    @State private var feedbackGiven: Set<String> = []

    var isFav: Bool { store.isFavorited(entry) }

    var body: some View {
        NavigationStack {
            ScrollView {
                ResultCard(text: entry.response, entry: entry, feedbackGiven: $feedbackGiven)
                    .padding(16)
            }
            .navigationTitle(entry.query)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "history.closeButton")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        store.toggleFavorite(entry)
                    } label: {
                        Image(systemName: isFav ? "star.fill" : "star")
                            .foregroundColor(Color.accentOrange)
                    }
                }
            }
        }
    }
}

// MARK: - Empty State
struct EmptyState: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 52))
                .foregroundColor(.secondary.opacity(0.5))
            Text(title)
                .font(.system(size: 18, weight: .semibold))
            Text(subtitle)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(40)
    }
}
