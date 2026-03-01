package com.jacquotq.lekker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacquotq.lekker.data.AppStorage
import com.jacquotq.lekker.data.FavoriteEntry
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.theme.*

@Composable
fun FlashcardScreen(
    storage: AppStorage,
    tts: TTSService,
    onNavigateToWord: (String) -> Unit = {}
) {
    var favorites by remember { mutableStateOf(storage.loadFavorites()) }
    val settings = remember { storage.loadSettings() }
    val isZh = settings.language == "zh"

    // Theme-aware colours
    val bgColor      = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor  = MaterialTheme.colorScheme.outline
    val textColor    = MaterialTheme.colorScheme.onSurface
    val hintColor    = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Page header ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                if (isZh) "闪卡练习" else "Flashcards",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                if (isZh) "间隔重复，高效记忆" else "Spaced repetition for effective learning.",
                fontSize = 14.sp,
                color = hintColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        HorizontalDivider(color = borderColor)

        if (favorites.isEmpty()) {
            EmptyFlashcards(isZh, hintColor)
        } else {
            FlashcardView(
                favorites = favorites,
                tts = tts,
                isZh = isZh,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                textColor = textColor,
                hintColor = hintColor,
                onUpdate = { updated ->
                    val favs = favorites.toMutableList()
                    val idx = favs.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) favs[idx] = updated
                    storage.saveFavorites(favs)
                    favorites = favs
                },
                onDelete = { entry ->
                    val favs = favorites.toMutableList().also { it.remove(entry) }
                    storage.saveFavorites(favs)
                    favorites = favs
                },
                onNavigateToWord = onNavigateToWord
            )
        }
    }
}

@Composable
private fun EmptyFlashcards(
    isZh: Boolean,
    hintColor: androidx.compose.ui.graphics.Color
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📚", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                if (isZh) "暂无闪卡" else "No flashcards yet",
                fontSize = 15.sp,
                color = hintColor
            )
            Text(
                if (isZh) "在查询结果中点击 ★ 收藏单词来创建闪卡"
                else "Star words in search results to create flashcards.",
                fontSize = 13.sp,
                color = hintColor
            )
        }
    }
}

/**
 * Daily review completed screen - shown when no cards are due today.
 */
@Composable
private fun DailyReviewCompleted(
    isZh: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    totalCards: Int,
    dueCards: Int
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🌴", fontSize = 64.sp)
            Text(
                if (isZh) "今日复习完成！" else "All done for today!",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                if (isZh) "没有需要复习的单词了\n享受生活吧！🎉"
                else "No cards due for review.\nEnjoy your day! 🎉",
                fontSize = 15.sp,
                color = hintColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            // Stats
            Text(
                if (isZh) "📚 收藏总数: $totalCards"
                else "📚 Total cards: $totalCards",
                fontSize = 13.sp,
                color = hintColor
            )
        }
    }
}

/**
 * Familiarity indicator: a small colored square based on interval.
 * 🔴 New (interval=1), 🟡 Learning (2-6), 🟢 Familiar (>6)
 */
@Composable
private fun FamiliaritySquare(interval: Int) {
    val color = when {
        interval <= 1 -> TagRedText
        interval <= 6 -> TagOrangeText
        else -> NotionAccentGreen
    }
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color)
    )
}

@Composable
private fun FlashcardView(
    favorites: List<FavoriteEntry>,
    tts: TTSService,
    isZh: Boolean,
    bgColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    onUpdate: (FavoriteEntry) -> Unit,
    onDelete: (FavoriteEntry) -> Unit,
    onNavigateToWord: (String) -> Unit = {}
) {
    val now = System.currentTimeMillis()
    
    // Only show cards that are due for review (nextReview <= now)
    val dueCards = remember(favorites, now) {
        favorites.filter { it.nextReview <= now }
            .sortedBy { it.nextReview }
    }
    
    // Track which cards have been reviewed in this session (by ID)
    var reviewedIds by remember { mutableStateOf(setOf<String>()) }
    
    // Cards remaining to review = due cards minus already reviewed ones
    val remainingCards = remember(dueCards, reviewedIds) {
        dueCards.filter { it.id !in reviewedIds }
    }

    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    // Reset index if it goes out of bounds
    LaunchedEffect(remainingCards.size) {
        if (remainingCards.isNotEmpty() && index >= remainingCards.size) {
            index = remainingCards.size - 1
        }
    }

    // ── No cards due - show "enjoy your day" screen ──────
    if (dueCards.isEmpty()) {
        DailyReviewCompleted(
            isZh = isZh,
            textColor = textColor,
            hintColor = hintColor,
            totalCards = favorites.size,
            dueCards = 0
        )
        return
    }

    // ── All due cards reviewed - session complete ────────
    if (remainingCards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text("🎉", fontSize = 64.sp)
                Text(
                    if (isZh) "太棒了！" else "Well done!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    if (isZh) "你已经复习完今天所有 ${dueCards.size} 张闪卡！\n去享受生活吧～"
                    else "You've reviewed all ${dueCards.size} cards due today!\nTime to enjoy life ☀️",
                    fontSize = 15.sp,
                    color = hintColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(16.dp))
                // Stats
                Text(
                    if (isZh) "📚 收藏总数: ${favorites.size}"
                    else "📚 Total cards: ${favorites.size}",
                    fontSize = 13.sp,
                    color = hintColor
                )
            }
        }
        return
    }

    val entry = remainingCards[index.coerceIn(0, remainingCards.size - 1)]
    val reviewedCount = reviewedIds.size
    val totalDue = dueCards.size

    // Advance to next card and record rating
    fun advanceAfterRating(quality: Int) {
        onUpdate(rateCard(entry, quality))
        // Mark this card as reviewed so it won't appear again
        reviewedIds = reviewedIds + entry.id
        flipped = false
        // Index will auto-adjust via LaunchedEffect when remainingCards changes
        if (index >= remainingCards.size - 1) {
            index = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Progress ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isZh) "今日进度: ${reviewedCount + 1} / $totalDue"
                else "Today: ${reviewedCount + 1} / $totalDue",
                fontSize = 13.sp,
                color = hintColor
            )
            NotionTag(
                if (isZh) "待复习" else "due",
                TagOrangeBg, TagOrangeText
            )
        }

        // Progress bar
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (reviewedCount + 1).toFloat() / totalDue },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = textColor,
            trackColor = borderColor
        )

        Spacer(Modifier.height(16.dp))

        // ── Card ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceColor)
        ) {
            if (!flipped) {
                // Front — just the word + familiarity square
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Familiarity indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FamiliaritySquare(entry.interval)
                            Text(
                                text = entry.query,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = textColor
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = { flipped = true }) {
                            Text(
                                if (isZh) "翻转查看" else "Tap to reveal",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                // Back — extracted meaning + link to full definition
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Word with familiarity square
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FamiliaritySquare(entry.interval)
                        Text(
                            text = entry.query,
                            fontWeight = FontWeight.Normal,
                            color = textColor,
                            fontSize = 16.sp
                        )
                    }

                    HorizontalDivider(color = borderColor)

                    // Extracted simple translation (~10 chars/words)
                    val meaning = remember(entry.id, isZh) {
                        extractSimpleTranslation(entry.response, isZh)
                    }
                    Text(
                        text = meaning,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.weight(1f))

                    // Link to full definition in Search screen
                    Text(
                        text = if (isZh) "查看完整释义 →" else "View full definition →",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { onNavigateToWord(entry.query) }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Navigation + rating ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous (only within remaining cards)
            TextButton(
                onClick = { 
                    index = (index - 1 + remainingCards.size) % remainingCards.size
                    flipped = false 
                },
                enabled = remainingCards.size > 1
            ) {
                Text("← " + if (isZh) "上一个" else "Prev", fontSize = 13.sp, color = hintColor)
            }

            if (flipped) {
                // SM-2 rating buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Again
                    OutlinedButton(
                        onClick = { advanceAfterRating(0) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TagRedText
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(if (isZh) "再来" else "Again", fontSize = 13.sp) }

                    // Good
                    Button(
                        onClick = { advanceAfterRating(3) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NotionAccentBlue,
                            contentColor = surfaceColor
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(if (isZh) "一般" else "Good", fontSize = 13.sp) }

                    // Easy
                    Button(
                        onClick = { advanceAfterRating(5) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NotionAccentGreen,
                            contentColor = surfaceColor
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(if (isZh) "简单" else "Easy", fontSize = 13.sp) }
                }
            } else {
                // Delete when not flipped
                TextButton(onClick = {
                    onDelete(entry)
                    if (index >= remainingCards.size - 1 && index > 0) index--
                    flipped = false
                }) {
                    Text("×  " + if (isZh) "删除" else "Remove", fontSize = 13.sp, color = hintColor)
                }
            }

            // Next (only within remaining cards)
            TextButton(
                onClick = { 
                    index = (index + 1) % remainingCards.size
                    flipped = false 
                },
                enabled = remainingCards.size > 1
            ) {
                Text((if (isZh) "下一个" else "Next") + " →", fontSize = 13.sp, color = hintColor)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Extract a simple, short translation from the AI response.
 * Target: ~10 Chinese characters or ~10 English words.
 * 
 * Looks for patterns like "基本释义：好吃的" and extracts content after the colon.
 */
private fun extractSimpleTranslation(response: String, isZh: Boolean): String {
    // Remove emojis
    val noEmoji = response.replace(Regex("[\\p{So}\\p{Cs}]"), "")
    
    // Labels that might precede the actual translation (with colon)
    val labels = listOf(
        "基本释义", "基础释义", "简单定义", "定义", "释义", "含义", "意思", "词义",
        "Definition", "Meaning", "Simple Definition", "Translation"
    )
    
    // Try to find "Label: content" or "Label：content" pattern
    for (label in labels) {
        val pattern = Regex("$label[：:]\\s*(.+)", RegexOption.IGNORE_CASE)
        val match = pattern.find(noEmoji)
        if (match != null) {
            val content = match.groupValues[1]
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\*(.+?)\\*"), "$1")
                .replace(Regex("^#+\\s*"), "")
                .trim()
            // Take up to first newline or sentence break
            val firstPart = content.split(Regex("[\\n。.！!？?]")).firstOrNull()?.trim()
            if (!firstPart.isNullOrBlank() && firstPart.length >= 2) {
                return truncateTranslation(firstPart, isZh)
            }
        }
    }
    
    // Fallback: clean up and find first meaningful line with target language
    val lines = noEmoji.lines()
        .map { line ->
            line
                .replace(Regex("^#+\\s*"), "")
                .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                .replace(Regex("\\*(.+?)\\*"), "$1")
                .replace(Regex("_(.+?)_"), "$1")
                .replace(Regex("^[-*>•]\\s*"), "")
                .replace(Regex("^\\d+\\.\\s*"), "")
                .trim()
        }
        .filter { it.isNotBlank() && it.length >= 2 }
        // Skip lines that are just labels
        .filter { line ->
            val lower = line.lowercase()
            !labels.any { l -> lower == l.lowercase() || lower.startsWith(l.lowercase()) }
        }
    
    fun hasCjk(s: String) = s.any { it.code in 0x4E00..0x9FFF }
    
    val candidates = if (isZh) {
        lines.filter { hasCjk(it) }
    } else {
        lines.filter { !hasCjk(it) }
    }
    
    val best = candidates.firstOrNull() ?: lines.firstOrNull()
    
    return truncateTranslation(best ?: (if (isZh) "暂无翻译" else "No translation"), isZh)
}

/** Truncate to ~10 chars (Chinese) or ~10 words (English) */
private fun truncateTranslation(text: String, isZh: Boolean): String {
    val clean = text
        .replace(Regex("^[,，、;；:\\s]+"), "")
        .replace(Regex("[,，、;；:\\s]+$"), "")
        .trim()
    
    return if (isZh) {
        if (clean.length > 15) clean.take(12) + "…" else clean
    } else {
        val words = clean.split(Regex("\\s+"))
        if (words.size > 10) words.take(10).joinToString(" ") + "…" else clean
    }
}

/** SM-2 spaced-repetition rating.  quality: 0=again, 3=ok, 5=easy */
private fun rateCard(entry: FavoriteEntry, quality: Int): FavoriteEntry {
    val newEase = when {
        quality >= 5 -> (entry.easeFactor + 0.1).coerceAtLeast(1.3)
        quality >= 3 -> entry.easeFactor.coerceAtLeast(1.3)
        else -> (entry.easeFactor - 0.2).coerceAtLeast(1.3)
    }
    val newInterval = when {
        quality < 3 -> 1
        entry.interval <= 1 -> 6
        else -> (entry.interval * entry.easeFactor).toInt().coerceAtLeast(1)
    }
    val nextReview = System.currentTimeMillis() + newInterval.toLong() * 86_400_000L
    return entry.copy(interval = newInterval, easeFactor = newEase, nextReview = nextReview)
}
