import Foundation
import SwiftUI

// MARK: - Central App Store (ObservableObject)
@MainActor
final class AppStore: ObservableObject {

    // ── Published State ──────────────────────────
    @Published var history:   [WordEntry] = []
    @Published var favorites: [WordEntry] = []
    @Published var profile:    UserProfile = UserProfile()
    @Published var settings:   AppSettings = AppSettings()
    @Published var flashcards: [FlashcardItem] = []

    // ── Cache: query → response ──────────────────
    private var cache: [String: String] = [:]

    // ── Persistence Keys ─────────────────────────
    private let historyKey   = "nl_history"
    private let favoritesKey = "nl_favorites"
    private let profileKey   = "nl_profile"
    private let settingsKey  = "nl_settings"
    private let cacheKey       = "nl_cache"
    private let flashcardsKey  = "nl_flashcards"

    // ── Device ID (stable per-device UUID) ───────
    var deviceId: String {
        let key = "nl_device_id"
        if let existing = UserDefaults.standard.string(forKey: key) { return existing }
        let newId = UUID().uuidString
        UserDefaults.standard.set(newId, forKey: key)
        return newId
    }

    // ── Language code for backend ────────────────
    var languageCode: String {
        switch settings.language {
        case .zhHans: return "zh"
        case .en:     return "en"
        case .system:
            let preferred = Locale.preferredLanguages.first ?? "en"
            return preferred.hasPrefix("zh") ? "zh" : "en"
        }
    }

    // ─────────────────────────────────────────────
    init() { load() }

    // ─────────────────────────────────────────────
    // MARK: Search / Chat
    // ─────────────────────────────────────────────
    func search(query: String) -> AsyncThrowingStream<String, Error> {
        // Cache hit
        let cacheKey = query.lowercased()
        if settings.useCache, let cached = cache[cacheKey] {
            let entry = WordEntry(
                id: ISO8601DateFormatter().string(from: Date()),
                query: query, response: cached,
                timestamp: Date(), model: "qwen-turbo", cached: true
            )
            addToHistory(entry)
            return AsyncThrowingStream { cont in
                cont.yield(cached); cont.finish()
            }
        }

        // Live API call — accumulate for history
        return AsyncThrowingStream { [weak self] continuation in
            guard let self else { continuation.finish(); return }
            Task { @MainActor in
                var full = ""
                let stream = AIService.streamChat(
                    query: query,
                    profile: self.profile,
                    language: self.languageCode,
                    deviceId: self.deviceId
                )
                do {
                    for try await chunk in stream {
                        full += chunk
                        continuation.yield(chunk)
                    }
                    let entry = WordEntry(
                        id: ISO8601DateFormatter().string(from: Date()),
                        query: query, response: full,
                        timestamp: Date(), model: "qwen-turbo", cached: false
                    )
                    self.addToHistory(entry)
                    // Update cache
                    self.cache[cacheKey] = full
                    if self.cache.count > 300 { self.cache.removeValue(forKey: self.cache.keys.first!) }
                    self.saveCache()
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // MARK: History
    // ─────────────────────────────────────────────
    func addToHistory(_ entry: WordEntry) {
        history.removeAll { $0.id == entry.id }
        history.insert(entry, at: 0)
        if history.count > 200 { history = Array(history.prefix(200)) }
        saveHistory()
    }

    func deleteHistory(at offsets: IndexSet) {
        history.remove(atOffsets: offsets)
        saveHistory()
    }

    func clearHistory() { history.removeAll(); saveHistory() }

    // ─────────────────────────────────────────────
    // MARK: Favorites
    // ─────────────────────────────────────────────
    func isFavorited(_ entry: WordEntry) -> Bool {
        favorites.contains { $0.id == entry.id }
    }

    func toggleFavorite(_ entry: WordEntry) {
        if isFavorited(entry) {
            favorites.removeAll { $0.id == entry.id }
        } else {
            favorites.insert(entry, at: 0)
        }
        saveFavorites()
        syncFlashcards()
    }

    func deleteFavorite(at offsets: IndexSet) {
        favorites.remove(atOffsets: offsets)
        saveFavorites()
        syncFlashcards()
    }

    // ─────────────────────────────────────────────
    // MARK: Flashcards (间隔复习)
    // ─────────────────────────────────────────────

    /// Sync flashcards with current favorites list
    func syncFlashcards() {
        let favoriteIDs = Set(favorites.map(\.id))
        let existingIDs = Set(flashcards.map(\.id))

        // Add new cards for favorites that don't have a flashcard yet
        for fav in favorites where !existingIDs.contains(fav.id) {
            let card = FlashcardItem(
                id: fav.id,
                query: fav.query,
                response: fav.response,
                familiarity: .unknown,
                nextReviewDate: Date(),
                currentInterval: 60,
                reviewCount: 0,
                lastReviewedDate: nil
            )
            flashcards.append(card)
        }

        // Remove cards whose words are no longer favorited
        flashcards.removeAll { !favoriteIDs.contains($0.id) }
        saveFlashcards()
    }

    /// All cards currently due for review
    var dueCards: [FlashcardItem] {
        flashcards.filter(\.isDue).sorted { $0.nextReviewDate < $1.nextReviewDate }
    }

    var dueCount: Int { dueCards.count }

    /// Review a card with a familiarity rating
    func reviewCard(id: String, rating: Familiarity) {
        guard let idx = flashcards.firstIndex(where: { $0.id == id }) else { return }

        let now = Date()
        flashcards[idx].familiarity = rating
        flashcards[idx].reviewCount += 1
        flashcards[idx].lastReviewedDate = now

        switch rating {
        case .unknown:
            flashcards[idx].currentInterval = 60
            flashcards[idx].nextReviewDate = now.addingTimeInterval(60)
        case .fuzzy:
            flashcards[idx].currentInterval = 4 * 3600
            flashcards[idx].nextReviewDate = now.addingTimeInterval(4 * 3600)
        case .known:
            let newInterval = min(
                max(flashcards[idx].currentInterval * 1.5, 86400),
                30 * 86400
            )
            flashcards[idx].currentInterval = newInterval
            flashcards[idx].nextReviewDate = now.addingTimeInterval(newInterval)
        }

        saveFlashcards()
    }

    func resetFlashcards() {
        flashcards.removeAll()
        syncFlashcards()
    }

    // ─────────────────────────────────────────────
    // MARK: Feedback & Profile
    // ─────────────────────────────────────────────
    func submitFeedback(_ section: FeedbackSection, positive: Bool) {
        profile.adjust(section, positive: positive)
        saveProfile()
    }

    func resetProfile() { profile = UserProfile(); saveProfile() }

    // ─────────────────────────────────────────────
    // MARK: Settings
    // ─────────────────────────────────────────────
    func saveSettings(_ s: AppSettings) { settings = s; persist(s, key: settingsKey) }

    // ─────────────────────────────────────────────
    // MARK: Persistence Helpers
    // ─────────────────────────────────────────────
    private func load() {
        history   = loadDecoded([WordEntry].self, key: historyKey)   ?? []
        favorites = loadDecoded([WordEntry].self, key: favoritesKey) ?? []
        profile   = loadDecoded(UserProfile.self, key: profileKey)   ?? UserProfile()
        settings  = loadDecoded(AppSettings.self, key: settingsKey)  ?? AppSettings()
        cache      = loadDecoded([String:String].self, key: cacheKey) ?? [:]
        flashcards = loadDecoded([FlashcardItem].self, key: flashcardsKey) ?? []
    }

    private func saveHistory()   { persist(history,   key: historyKey) }
    private func saveFavorites() { persist(favorites,  key: favoritesKey) }
    private func saveProfile()   { persist(profile,    key: profileKey) }
    private func saveCache()       { persist(cache,       key: cacheKey) }
    private func saveFlashcards()  { persist(flashcards,  key: flashcardsKey) }

    private func persist<T: Encodable>(_ value: T, key: String) {
        if let data = try? JSONEncoder().encode(value) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    private func loadDecoded<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}
