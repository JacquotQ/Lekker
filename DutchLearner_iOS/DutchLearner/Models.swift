import Foundation
import SwiftUI

// MARK: - App Colors
extension Color {
    static let accentOrange = Color(red: 1.0, green: 0.49, blue: 0.22)
}

// MARK: - Word Entry (历史/收藏记录)
struct WordEntry: Identifiable, Codable, Equatable {
    var id: String
    var query: String
    var response: String
    var timestamp: Date
    var model: String
    var cached: Bool

    var modelShort: String {
        return "Qwen"
    }
    var relativeTime: String {
        let diff = Date().timeIntervalSince(timestamp)
        switch diff {
        case ..<60:       return String(localized: "time.justNow")
        case ..<3600:     return String(localized: "time.minutesAgo \(Int(diff/60))")
        case ..<86400:    return String(localized: "time.hoursAgo \(Int(diff/3600))")
        case ..<604800:   return String(localized: "time.daysAgo \(Int(diff/86400))")
        default:
            let f = DateFormatter(); f.dateStyle = .medium
            return f.string(from: timestamp)
        }
    }
}

// MARK: - User Learning Profile
struct UserProfile: Codable {
    var etymologyWeight: Double = 1.0
    var storiesWeight: Double  = 1.0
    var connectionsWeight: Double = 1.0
    var formalWeight: Double   = 1.0

    mutating func adjust(_ section: FeedbackSection, positive: Bool) {
        let factor: Double = positive ? 1.1 : 0.9
        switch section {
        case .etymology:   etymologyWeight   = max(0.3, min(2.0, (etymologyWeight   * factor * 1000).rounded() / 1000))
        case .stories:     storiesWeight     = max(0.3, min(2.0, (storiesWeight     * factor * 1000).rounded() / 1000))
        case .connections: connectionsWeight = max(0.3, min(2.0, (connectionsWeight * factor * 1000).rounded() / 1000))
        case .formal:      formalWeight      = max(0.3, min(2.0, (formalWeight      * factor * 1000).rounded() / 1000))
        }
    }

    var summaryLines: [String] {
        let map: [(String, Double)] = [
            (String(localized: "profile.etymology"), etymologyWeight),
            (String(localized: "profile.stories"), storiesWeight),
            (String(localized: "profile.connections"), connectionsWeight),
            (String(localized: "profile.formal"), formalWeight),
        ]
        return map.map { label, w in
            let pct = Int((w - 1) * 100)
            let icon = w > 1.2 ? "📈" : w < 0.8 ? "📉" : "➡️"
            return "\(icon) \(label)：\(pct >= 0 ? "+" : "")\(pct)%"
        }
    }
}

// MARK: - Feedback Section
enum FeedbackSection: String, CaseIterable {
    case etymology    = "etymology"
    case stories      = "stories"
    case connections  = "connections"
    case formal       = "formal"

    var label: String {
        switch self {
        case .etymology:    return String(localized: "profile.etymology")
        case .stories:      return String(localized: "profile.stories")
        case .connections:  return String(localized: "profile.connections")
        case .formal:       return String(localized: "profile.formal")
        }
    }
}

// MARK: - Flashcard (间隔复习)
enum Familiarity: Int, Codable, CaseIterable {
    case unknown = 0   // 不认识
    case fuzzy   = 1   // 模糊
    case known   = 2   // 认识

    var label: String {
        switch self {
        case .unknown: return String(localized: "familiarity.unknown")
        case .fuzzy:   return String(localized: "familiarity.fuzzy")
        case .known:   return String(localized: "familiarity.known")
        }
    }

    var icon: String {
        switch self {
        case .unknown: return "xmark.circle"
        case .fuzzy:   return "questionmark.circle"
        case .known:   return "checkmark.circle"
        }
    }

    var color: Color {
        switch self {
        case .unknown: return .red
        case .fuzzy:   return .orange
        case .known:   return .green
        }
    }
}

struct FlashcardItem: Identifiable, Codable, Equatable {
    var id: String
    var query: String
    var response: String
    var familiarity: Familiarity
    var nextReviewDate: Date
    var currentInterval: TimeInterval
    var reviewCount: Int
    var lastReviewedDate: Date?

    var isDue: Bool { Date() >= nextReviewDate }
}

// MARK: - App Language
enum AppLanguage: String, Codable, CaseIterable {
    case system = "system"
    case zhHans = "zh-Hans"
    case en     = "en"

    var displayName: String {
        switch self {
        case .system: return String(localized: "language.system")
        case .zhHans: return "中文"
        case .en:     return "English"
        }
    }

    /// Returns the Locale to apply, or nil for system default.
    var locale: Locale? {
        switch self {
        case .system: return nil
        case .zhHans: return Locale(identifier: "zh-Hans")
        case .en:     return Locale(identifier: "en")
        }
    }
}

// MARK: - App Settings
struct AppSettings: Codable {
    var useCache: Bool = true
    var language: AppLanguage = .system
}
