import Foundation

// MARK: - AI Service (Backend Proxy)
final class AIService {

    // Backend URL — update after deploying Cloudflare Worker
    private static let backendURL = URL(string: "https://lekker-api.jonstark186.workers.dev/api/query")!

    // ──────────────────────────────────────────────
    // MARK: Streaming Entry Point
    // ──────────────────────────────────────────────
    static func streamChat(
        query: String,
        profile: UserProfile,
        language: String,
        deviceId: String
    ) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    var request = URLRequest(url: backendURL)
                    request.httpMethod = "POST"
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

                    let body: [String: Any] = [
                        "query": query,
                        "deviceId": deviceId,
                        "language": language,
                        "profile": [
                            "etymologyWeight": profile.etymologyWeight,
                            "storiesWeight": profile.storiesWeight,
                            "connectionsWeight": profile.connectionsWeight,
                            "formalWeight": profile.formalWeight,
                        ],
                    ]
                    request.httpBody = try JSONSerialization.data(withJSONObject: body)

                    let (bytes, response) = try await URLSession.shared.bytes(for: request)
                    guard let http = response as? HTTPURLResponse else {
                        throw AIServiceError.invalidResponse
                    }
                    if http.statusCode == 429 {
                        throw AIServiceError.rateLimited
                    }
                    guard http.statusCode == 200 else {
                        throw AIServiceError.httpError(http.statusCode)
                    }

                    // Parse OpenAI-compatible SSE stream
                    for try await line in bytes.lines {
                        guard line.hasPrefix("data: ") else { continue }
                        let jsonStr = String(line.dropFirst(6))
                        guard jsonStr != "[DONE]",
                              let data = jsonStr.data(using: .utf8),
                              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                              let choices = obj["choices"] as? [[String: Any]],
                              let first = choices.first,
                              let delta = first["delta"] as? [String: Any],
                              let text = delta["content"] as? String
                        else { continue }
                        continuation.yield(text)
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}

// MARK: - Errors
enum AIServiceError: LocalizedError {
    case invalidResponse
    case httpError(Int)
    case rateLimited

    var errorDescription: String? {
        switch self {
        case .invalidResponse:  return String(localized: "error.invalidResponse")
        case .httpError(let c): return String(localized: "error.httpError \(c)")
        case .rateLimited:      return String(localized: "error.rateLimited")
        }
    }
}
