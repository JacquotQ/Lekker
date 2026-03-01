import SwiftUI

// MARK: - Flashcard Tab (Root)
struct FlashcardView: View {
    @EnvironmentObject var store: AppStore
    @State private var showSession = false

    var body: some View {
        NavigationStack {
            Group {
                if store.favorites.isEmpty {
                    EmptyState(
                        icon: "rectangle.on.rectangle.angled",
                        title: String(localized: "flashcard.emptyTitle"),
                        subtitle: String(localized: "flashcard.emptySubtitle")
                    )
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            statsCard

                            if store.dueCount > 0 {
                                Button {
                                    showSession = true
                                } label: {
                                    HStack(spacing: 8) {
                                        Image(systemName: "play.fill")
                                        Text("flashcard.startReview \(store.dueCount)")
                                    }
                                    .font(.system(size: 17, weight: .semibold))
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(Color.accentOrange)
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                }
                            } else {
                                allDoneCard
                            }

                            cardsList
                        }
                        .padding(16)
                    }
                }
            }
            .navigationTitle(String(localized: "flashcard.navTitle"))
            .navigationBarTitleDisplayMode(.large)
            .onAppear { store.syncFlashcards() }
            .fullScreenCover(isPresented: $showSession) {
                FlashcardSessionView()
                    .environmentObject(store)
            }
        }
    }

    // MARK: - Stats Card
    private var statsCard: some View {
        VStack(spacing: 12) {
            HStack {
                StatBadge(value: "\(store.dueCount)", label: String(localized: "flashcard.dueLabel"), color: .accentOrange)
                Spacer()
                StatBadge(
                    value: "\(store.flashcards.filter { $0.familiarity == .known }.count)",
                    label: String(localized: "flashcard.masteredLabel"), color: .green
                )
                Spacer()
                StatBadge(value: "\(store.flashcards.count)", label: String(localized: "flashcard.totalLabel"), color: .secondary)
            }

            let total = store.flashcards.count
            let known = store.flashcards.filter { $0.familiarity == .known }.count
            if total > 0 {
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: "flashcard.progressLabel"))
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color(.systemGray5))
                                .frame(height: 8)
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.green)
                                .frame(width: geo.size.width * CGFloat(known) / CGFloat(total), height: 8)
                        }
                    }
                    .frame(height: 8)
                }
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.07), radius: 8, y: 2)
    }

    // MARK: - All Done Card
    private var allDoneCard: some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 36))
                .foregroundColor(.green)
            Text(String(localized: "flashcard.allDone"))
                .font(.system(size: 16, weight: .semibold))
            if let next = store.flashcards
                .filter({ !$0.isDue })
                .sorted(by: { $0.nextReviewDate < $1.nextReviewDate })
                .first {
                Text("flashcard.nextReview \(nextReviewText(next.nextReviewDate))")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .shadow(color: .black.opacity(0.07), radius: 8, y: 2)
    }

    // MARK: - Cards List
    private var cardsList: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "flashcard.allCards"))
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(.secondary)
                .textCase(.uppercase)
                .padding(.horizontal, 4)

            ForEach(store.flashcards) { card in
                HStack(spacing: 12) {
                    Image(systemName: card.familiarity.icon)
                        .foregroundColor(card.familiarity.color)
                        .font(.system(size: 18))
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(card.query)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.primary)
                        Text(card.isDue ? String(localized: "flashcard.dueStatus") : String(localized: "flashcard.nextStatus \(nextReviewText(card.nextReviewDate))"))
                            .font(.system(size: 12))
                            .foregroundColor(card.isDue ? Color.accentOrange : .secondary)
                    }

                    Spacer()

                    SpeakButton(text: card.query, iconSize: 15)

                    if card.isDue {
                        Circle()
                            .fill(Color.accentOrange)
                            .frame(width: 8, height: 8)
                    }
                }
                .padding(12)
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
            }
        }
    }

    // MARK: - Helper
    private func nextReviewText(_ date: Date) -> String {
        let diff = date.timeIntervalSince(Date())
        if diff <= 0 { return String(localized: "time.now") }
        if diff < 3600 { return String(localized: "time.minutesLater \(Int(diff / 60))") }
        if diff < 86400 { return String(localized: "time.hoursLater \(Int(diff / 3600))") }
        return String(localized: "time.daysLater \(Int(diff / 86400))")
    }
}

// MARK: - Stat Badge
private struct StatBadge: View {
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(color)
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Flashcard Session View
struct FlashcardSessionView: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) var dismiss

    @State private var cards: [FlashcardItem] = []
    @State private var currentIndex = 0
    @State private var isFlipped = false
    @State private var sessionComplete = false
    @State private var reviewedCount = 0

    var currentCard: FlashcardItem? {
        guard currentIndex < cards.count else { return nil }
        return cards[currentIndex]
    }

    var body: some View {
        NavigationStack {
            Group {
                if sessionComplete {
                    sessionCompleteView
                } else if let card = currentCard {
                    cardReviewView(card)
                }
            }
            .navigationTitle(String(localized: "session.navTitle"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "session.endButton")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Text("\(currentIndex + 1) / \(cards.count)")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.secondary)
                }
            }
        }
        .onAppear {
            cards = store.dueCards
            if cards.isEmpty { sessionComplete = true }
        }
    }

    // MARK: - Card Review
    @ViewBuilder
    private func cardReviewView(_ card: FlashcardItem) -> some View {
        VStack(spacing: 0) {
            ProgressView(value: Double(currentIndex), total: Double(cards.count))
                .tint(Color.accentOrange)
                .padding(.horizontal, 16)
                .padding(.top, 8)

            Spacer()

            // The Card
            if !isFlipped {
                // FRONT: Dutch word
                VStack(spacing: 16) {
                    Text(card.query)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(Color.accentOrange)
                        .multilineTextAlignment(.center)

                    SpeakButton(text: card.query, iconSize: 24)

                    Text(String(localized: "session.tapHint"))
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                .padding(30)
                .frame(maxWidth: .infinity, minHeight: 300)
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .shadow(color: .black.opacity(0.07), radius: 8, y: 2)
                .padding(.horizontal, 16)
                .onTapGesture {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        isFlipped = true
                    }
                }
            } else {
                // BACK: AI explanation
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text(card.query)
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(Color.accentOrange)
                            SpeakButton(text: card.query, iconSize: 18)
                        }

                        Divider()

                        MarkdownText(text: card.response)
                    }
                    .padding(20)
                }
                .frame(maxWidth: .infinity, maxHeight: 400)
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 18))
                .shadow(color: .black.opacity(0.07), radius: 8, y: 2)
                .padding(.horizontal, 16)
            }

            Spacer()

            // Rating buttons (only when flipped)
            if isFlipped {
                VStack(spacing: 8) {
                    Text(String(localized: "session.ratingPrompt"))
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)

                    HStack(spacing: 12) {
                        ForEach(Familiarity.allCases, id: \.self) { rating in
                            Button {
                                rateCard(card, rating: rating)
                            } label: {
                                VStack(spacing: 4) {
                                    Image(systemName: rating.icon)
                                        .font(.system(size: 22))
                                    Text(rating.label)
                                        .font(.system(size: 13, weight: .semibold))
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(rating.color.opacity(0.15))
                                .foregroundColor(rating.color)
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    // MARK: - Session Complete
    private var sessionCompleteView: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 56))
                .foregroundColor(Color.accentOrange)

            Text(String(localized: "session.complete"))
                .font(.system(size: 24, weight: .bold))

            Text("session.reviewedCount \(reviewedCount)")
                .font(.system(size: 15))
                .foregroundColor(.secondary)

            Button {
                dismiss()
            } label: {
                Text(String(localized: "session.backButton"))
                    .font(.system(size: 17, weight: .semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.accentOrange)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 40)
            .padding(.top, 12)
        }
        .padding(40)
    }

    // MARK: - Rating Logic
    private func rateCard(_ card: FlashcardItem, rating: Familiarity) {
        store.reviewCard(id: card.id, rating: rating)
        reviewedCount += 1

        withAnimation {
            isFlipped = false
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            if currentIndex + 1 < cards.count {
                currentIndex += 1
            } else {
                withAnimation {
                    sessionComplete = true
                }
            }
        }
    }
}
