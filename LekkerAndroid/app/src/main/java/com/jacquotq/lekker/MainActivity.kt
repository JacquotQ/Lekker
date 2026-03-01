package com.jacquotq.lekker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jacquotq.lekker.data.AppStorage
import com.jacquotq.lekker.service.AIService
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.theme.LekkerTheme

class MainActivity : ComponentActivity() {

    private lateinit var tts: TTSService
    private lateinit var storage: AppStorage
    private val ai = AIService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storage = AppStorage(this)
        tts = TTSService(this)
        setContent {
            LekkerTheme {
                LekkerApp(storage = storage, tts = tts, ai = ai)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
