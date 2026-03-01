import AVFoundation
import SwiftUI

// MARK: - Speech Service (singleton)
/// Wraps AVSpeechSynthesizer for Dutch (nl-NL) text-to-speech.
@MainActor
final class SpeechService: NSObject, ObservableObject {

    static let shared = SpeechService()

    @Published var isSpeaking  = false
    @Published var activeText: String? = nil   // which text is currently being spoken

    private let synth = AVSpeechSynthesizer()

    private override init() {
        super.init()
        synth.delegate = self
        // Configure audio session for playback alongside other audio
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: .duckOthers)
    }

    // ──────────────────────────────────────────────
    // MARK: Public API
    // ──────────────────────────────────────────────

    /// Toggle speaking `text`. If `text` is already being spoken, stop it.
    func toggle(_ text: String, rate: Float = 0.45) {
        if isSpeaking && activeText == text {
            stop()
            return
        }
        speak(text, rate: rate)
    }

    func speak(_ text: String, rate: Float = 0.45) {
        stop()
        let u = AVSpeechUtterance(string: text)
        u.voice          = AVSpeechSynthesisVoice(language: "nl-NL")
        u.rate           = rate          // 0.45 is ~80% of normal speed — good for learning
        u.pitchMultiplier = 1.0
        u.volume         = 1.0
        activeText = text
        synth.speak(u)
    }

    func stop() {
        synth.stopSpeaking(at: .immediate)
        // Delegate callbacks will reset state
    }

    /// True if `text` is exactly what's currently being spoken.
    func isActive(_ text: String) -> Bool {
        isSpeaking && activeText == text
    }
}

// MARK: - AVSpeechSynthesizerDelegate
extension SpeechService: AVSpeechSynthesizerDelegate {
    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didStart u: AVSpeechUtterance) {
        Task { @MainActor in self.isSpeaking = true }
    }
    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didFinish u: AVSpeechUtterance) {
        Task { @MainActor in self.isSpeaking = false; self.activeText = nil }
    }
    nonisolated func speechSynthesizer(_ s: AVSpeechSynthesizer, didCancel u: AVSpeechUtterance) {
        Task { @MainActor in self.isSpeaking = false; self.activeText = nil }
    }
}

// MARK: - SpeakButton View
/// A reusable speaker icon button that toggles Dutch TTS for a given text.
struct SpeakButton: View {
    let text: String
    var iconSize: CGFloat  = 17
    var tintActive: Color  = Color.accentOrange
    var tintIdle: Color    = .secondary

    @ObservedObject private var speech = SpeechService.shared

    private var active: Bool { speech.isActive(text) }

    var body: some View {
        Button {
            speech.toggle(text)
        } label: {
            Image(systemName: active ? "stop.circle.fill" : "speaker.wave.2.fill")
                .font(.system(size: iconSize))
                .foregroundStyle(active ? tintActive : tintIdle)
                .contentTransition(.symbolEffect(.replace))
                .symbolEffect(.variableColor.iterative.dimInactiveLayers, isActive: active)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(active ? String(localized: "speech.stopLabel") : String(localized: "speech.speakLabel"))
    }
}

// MARK: - Preview
#Preview {
    VStack(spacing: 20) {
        SpeakButton(text: "gezellig", iconSize: 24)
        SpeakButton(text: "Hoe gaat het met jou?", iconSize: 20)
    }
    .padding()
}
