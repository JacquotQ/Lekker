package com.jacquotq.lekker.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TTSService(context: Context) {

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private var activeText: String? = null
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("nl", "NL")
                tts?.setSpeechRate(0.85f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { _speaking.value = true }
                    override fun onDone(id: String?) { _speaking.value = false; activeText = null }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) { _speaking.value = false; activeText = null }
                })
                ready = true
            }
        }
    }

    fun toggle(text: String, slow: Boolean = false) {
        if (!ready) return
        if (activeText == text && tts?.isSpeaking == true) {
            stop()
            return
        }
        stop()
        activeText = text
        tts?.setSpeechRate(if (slow) 0.5f else 0.85f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lekker_tts")
    }

    fun stop() {
        tts?.stop()
        _speaking.value = false
        activeText = null
    }

    fun isSpeaking(text: String) = activeText == text && tts?.isSpeaking == true

    fun shutdown() { tts?.shutdown() }
}
