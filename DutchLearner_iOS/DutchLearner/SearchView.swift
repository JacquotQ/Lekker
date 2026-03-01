import SwiftUI

struct SearchView: View {
    @EnvironmentObject var store: AppStore
    @Binding var showSettings: Bool

    @State private var query = ""
    @State private var streaming = ""
    @State private var isLoading = false
    @State private var errorMsg: String? = nil
    @State private var currentEntry: WordEntry? = nil
    @State private var feedbackGiven: Set<String> = []
    @FocusState private var fieldFocused: Bool

    let quickWords = ["gezellig", "fiets", "lekker", "stroopwafel", "dankjewel", "huis", "mooi"]
    let randomWords = ["gezellig","fiets","lekker","stroopwafel","dankjewel","huis","water","mooi",
                       "groot","tijd","werken","gaan","tulp","kaas","gracht","rijksmuseum"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    // Quick chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(quickWords, id: \.self) { w in
                                ChipButton(title: w) { startSearch(w) }
                            }
                            ChipButton(title: String(localized: "search.randomChip")) { startSearch(randomWords.randomElement()!) }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.horizontal, -16)

                    // Search field
                    HStack(spacing: 8) {
                        TextField(String(localized: "search.placeholder"), text: $query, axis: .vertical)
                            .textFieldStyle(.plain)
                            .padding(12)
                            .background(Color(.systemGray6))
                            .clipShape(RoundedRectangle(cornerRadius: 13))
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .focused($fieldFocused)
                            .onSubmit { startSearch(query) }

                        Button {
                            fieldFocused = false
                            startSearch(query)
                        } label: {
                            Text(isLoading ? "…" : String(localized: "search.button"))
                                .font(.system(size: 15, weight: .semibold))
                                .padding(.horizontal, 18)
                                .padding(.vertical, 12)
                                .background(Color.accentOrange)
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 13))
                        }
                        .disabled(isLoading || query.trimmingCharacters(in: .whitespaces).isEmpty)
                    }

                    // Error
                    if let err = errorMsg {
                        Label(err, systemImage: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                            .font(.callout)
                            .padding(12)
                            .background(Color.red.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 11))
                    }

                    // Loading indicator
                    if isLoading && streaming.isEmpty {
                        HStack(spacing: 10) {
                            ProgressView().tint(Color.accentOrange)
                            Text(String(localized: "search.analyzing"))
                                .foregroundColor(.secondary)
                                .font(.callout)
                        }
                        .padding(14)
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: .black.opacity(0.07), radius: 6, y: 2)
                    }

                    // Response
                    if !streaming.isEmpty {
                        ResultCard(
                            text: streaming,
                            entry: currentEntry,
                            feedbackGiven: $feedbackGiven
                        )
                    }
                }
                .padding(16)
            }
            .navigationTitle(String(localized: "search.navTitle"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape.fill")
                            .foregroundColor(Color.accentOrange)
                    }
                }
            }
        }
    }

    // ──────────────────────────────────
    func startSearch(_ q: String) {
        let trimmed = q.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !isLoading else { return }

        query = trimmed
        streaming = ""
        errorMsg = nil
        currentEntry = nil
        feedbackGiven = []
        isLoading = true
        fieldFocused = false

        Task {
            do {
                let stream = store.search(query: trimmed)
                var full = ""
                for try await chunk in stream {
                    full += chunk
                    await MainActor.run { streaming = full }
                }
                await MainActor.run {
                    currentEntry = store.history.first { $0.query == trimmed }
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    errorMsg = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
}

// MARK: - Chip Button
struct ChipButton: View {
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13))
                .padding(.horizontal, 13)
                .padding(.vertical, 6)
                .background(Color(.systemBackground))
                .foregroundColor(.secondary)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(Color(.systemGray4), lineWidth: 1.2))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Result Card
struct ResultCard: View {
    let text: String
    let entry: WordEntry?
    @Binding var feedbackGiven: Set<String>
    @EnvironmentObject var store: AppStore
    @State private var copied = false

    var isFav: Bool { entry.map { store.isFavorited($0) } ?? false }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Markdown text
            MarkdownText(text: text)
                .padding(16)

            Divider().padding(.horizontal, 14)

            // Action buttons
            HStack(spacing: 8) {
                if let entry {
                    Button {
                        store.toggleFavorite(entry)
                    } label: {
                        Label(isFav ? String(localized: "result.favorited") : String(localized: "result.favorite"), systemImage: isFav ? "star.fill" : "star")
                            .font(.system(size: 13))
                    }
                    .buttonStyle(.bordered)
                    .tint(isFav ? Color.accentOrange : .secondary)
                }

                Button {
                    UIPasteboard.general.string = text
                    copied = true
                    DispatchQueue.main.asyncAfter(deadline: .now()+1.8) { copied = false }
                } label: {
                    Label(copied ? String(localized: "result.copied") : String(localized: "result.copy"), systemImage: copied ? "checkmark" : "doc.on.doc")
                        .font(.system(size: 13))
                }
                .buttonStyle(.bordered)
                .tint(.secondary)

                if let entry {
                    let isCached = entry.cached
                    if isCached {
                        Label(String(localized: "result.cached"), systemImage: "memorychip")
                            .font(.system(size: 11))
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(Color.green.opacity(0.15))
                            .foregroundColor(.green)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)

            Divider().padding(.horizontal, 14)

            // Feedback section
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "result.feedbackTitle"))
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 6) {
                    ForEach(FeedbackSection.allCases, id: \.self) { section in
                        FeedbackPill(section: section, feedbackGiven: $feedbackGiven)
                    }
                }
            }
            .padding(14)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.07), radius: 8, y: 2)
    }
}

// MARK: - Feedback Pill
struct FeedbackPill: View {
    let section: FeedbackSection
    @Binding var feedbackGiven: Set<String>
    @EnvironmentObject var store: AppStore

    var posKey: String { "pos_\(section.rawValue)" }
    var negKey: String { "neg_\(section.rawValue)" }

    var body: some View {
        HStack(spacing: 4) {
            Button {
                store.submitFeedback(section, positive: true)
                feedbackGiven.insert(posKey)
            } label: {
                HStack(spacing: 3) {
                    Image(systemName: feedbackGiven.contains(posKey) ? "hand.thumbsup.fill" : "hand.thumbsup")
                    Text(section.label).font(.system(size: 11))
                }
                .padding(.horizontal, 8).padding(.vertical, 5)
                .background(feedbackGiven.contains(posKey) ? Color.green.opacity(0.15) : Color(.systemGray6))
                .foregroundColor(feedbackGiven.contains(posKey) ? .green : .secondary)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .disabled(feedbackGiven.contains(posKey) || feedbackGiven.contains(negKey))

            Button {
                store.submitFeedback(section, positive: false)
                feedbackGiven.insert(negKey)
            } label: {
                Image(systemName: feedbackGiven.contains(negKey) ? "hand.thumbsdown.fill" : "hand.thumbsdown")
                    .font(.system(size: 11))
                    .padding(6)
                    .background(feedbackGiven.contains(negKey) ? Color.red.opacity(0.15) : Color(.systemGray6))
                    .foregroundColor(feedbackGiven.contains(negKey) ? .red : .secondary)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .disabled(feedbackGiven.contains(posKey) || feedbackGiven.contains(negKey))
        }
    }
}
