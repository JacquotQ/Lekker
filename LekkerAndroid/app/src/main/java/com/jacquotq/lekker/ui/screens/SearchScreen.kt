package com.jacquotq.lekker.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacquotq.lekker.data.*
import com.jacquotq.lekker.service.AIService
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.components.MarkdownViewer
import com.jacquotq.lekker.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    storage: AppStorage,
    tts: TTSService,
    ai: AIService,
    settings: AppSettings,
    /** When non-null, immediately display this word's cached result on launch. */
    initialQuery: String? = null
) {
    var query        by remember { mutableStateOf("") }
    var response     by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    var currentQuery by remember { mutableStateOf("") }
    var favorites    by remember { mutableStateOf(storage.loadFavorites()) }

    // Track which TTS mode is active: null = stopped, false = normal, true = slow
    var ttsSlowMode  by remember { mutableStateOf<Boolean?>(null) }

    val scrollState  = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val mainHandler  = remember { Handler(Looper.getMainLooper()) }
    val coroutineScope = rememberCoroutineScope()
    val isZh         = settings.language == "zh"

    // Observe TTS speaking state to sync button icons
    val isSpeaking by tts.speaking.collectAsState()
    // When TTS stops externally, reset our mode tracker
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking) ttsSlowMode = null
    }

    // Theme-aware colours
    val bgColor       = MaterialTheme.colorScheme.background
    val surfaceColor  = MaterialTheme.colorScheme.surface
    val borderColor   = MaterialTheme.colorScheme.outline
    val textColor     = MaterialTheme.colorScheme.onSurface
    val hintColor     = MaterialTheme.colorScheme.onSurfaceVariant

    // If a word was deep-linked from the flashcard screen, load it from cache immediately
    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && currentQuery != initialQuery) {
            val cached = storage.getCached(initialQuery)
                ?: storage.loadHistory().firstOrNull { it.query == initialQuery }?.response
            if (cached != null) {
                currentQuery = initialQuery
                response = cached
            } else {
                // Not in cache, trigger a live search
                query = initialQuery
            }
        }
    }

    val isFav = currentQuery.isNotEmpty() && favorites.any { it.query == currentQuery }
    val hasResults = response.isNotEmpty() || isLoading

    fun doSearch() {
        val w = query.trim()
        if (w.isBlank() || isLoading) return
        focusManager.clearFocus()
        tts.stop()
        ttsSlowMode = null
        currentQuery = w
        query = ""  // Clear the search bar immediately after searching
        isLoading = true
        response = ""
        errorMsg = null
        favorites = storage.loadFavorites()

        val profile  = storage.loadProfile()
        val deviceId = storage.getDeviceId()

        ai.streamQuery(
            query    = w,
            profile  = profile,
            language = settings.language,
            deviceId = deviceId,
            onToken  = { token -> mainHandler.post { response += token } },
            onDone   = {
                mainHandler.post {
                    isLoading = false
                    if (response.isNotEmpty()) {
                        val entry = HistoryEntry(query = w, response = response)
                        val history = storage.loadHistory()
                        history.removeAll { it.query == w }
                        history.add(0, entry)
                        while (history.size > 200) history.removeLast()
                        storage.saveHistory(history)
                    }
                }
            },
            onError  = { err -> mainHandler.post { isLoading = false; errorMsg = err } }
        )
    }

    // Trigger live search if initialQuery wasn't in cache
    LaunchedEffect(query) {
        if (query.isNotBlank() && !isLoading && currentQuery.isEmpty()) {
            doSearch()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (hasResults) {
            // ── When showing results: fixed header at top ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                    placeholder = {
                        Text(
                            if (isZh) "搜索另一个词..." else "Search another word...",
                            color = hintColor,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = borderColor,
                        focusedContainerColor   = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Word title + TTS buttons + favourite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentQuery,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (response.isNotEmpty()) {
                        // Normal-speed TTS 🔊
                        IconButton(onClick = {
                            if (isSpeaking && ttsSlowMode == false) {
                                tts.stop()          // tapping again stops playback
                            } else {
                                ttsSlowMode = false
                                tts.toggle(currentQuery, slow = false)
                            }
                        }) {
                            Text(
                                text = if (isSpeaking && ttsSlowMode == false) "⏹" else "🔊",
                                fontSize = 20.sp
                            )
                        }
                        // Slow TTS 🐢
                        IconButton(onClick = {
                            if (isSpeaking && ttsSlowMode == true) {
                                tts.stop()          // tapping again stops playback
                            } else {
                                ttsSlowMode = true
                                tts.toggle(currentQuery, slow = true)
                            }
                        }) {
                            Text(
                                text = if (isSpeaking && ttsSlowMode == true) "⏹" else "🐢",
                                fontSize = 20.sp
                            )
                        }
                        // Favourite button
                        IconButton(onClick = {
                            val favs = storage.loadFavorites()
                            if (isFav) {
                                favs.removeAll { it.query == currentQuery }
                            } else {
                                favs.add(0, FavoriteEntry(
                                    id        = UUID.randomUUID().toString(),
                                    query     = currentQuery,
                                    response  = response,
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                            storage.saveFavorites(favs)
                            favorites = favs
                        }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) NotionAccentOrange else hintColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = borderColor)

            // ── Scrollable results ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (isLoading && response.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color       = textColor,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(24.dp)
                        )
                    }
                } else {
                    MarkdownViewer(markdown = response, tts = tts)
                    if (isLoading)
                        Text("▋", color = hintColor, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            // ── Home / empty state: centered search bar ──────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Lekker",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (isZh) "从这里拿捏荷兰语" else "You will fluent in Netherlands",
                    fontSize = 14.sp,
                    color = hintColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
                )

                // Centered search bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 24.dp),
                    placeholder = {
                        Text(
                            if (isZh) "搜索荷兰语单词..." else "Search a Dutch word...",
                            color = hintColor
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor    = borderColor,
                        focusedContainerColor   = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )

                Spacer(Modifier.height(48.dp))

                // Suggestion chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    listOf("lekker", "gezellig", "alsjeblieft").forEach { word ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { query = word; doSearch() },
                            color = surfaceColor,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                word,
                                fontSize = 13.sp,
                                color = hintColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Recent
                val history = remember { storage.loadHistory().take(5) }
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(36.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        HorizontalDivider(color = borderColor)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isZh) "最近搜索" else "Recent",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hintColor,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        history.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = entry.query; doSearch() }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.query,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                NotionTag("cached", TagGreenBg, TagGreenText)
                            }
                            HorizontalDivider(color = borderColor)
                        }
                    }
                }
            }
        }

        // Error banner
        errorMsg?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TagRedBg)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("❌", fontSize = 14.sp)
                Text(err, fontSize = 14.sp, color = TagRedText)
            }
        }
    }
}

@Composable
fun NotionTag(
    text: String,
    bgColor: androidx.compose.ui.graphics.Color = TagOrangeBg,
    textColor: androidx.compose.ui.graphics.Color = TagOrangeText
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
