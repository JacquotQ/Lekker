package com.jacquotq.lekker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacquotq.lekker.data.AppSettings
import com.jacquotq.lekker.data.AppStorage
import com.jacquotq.lekker.data.FavoriteEntry
import com.jacquotq.lekker.data.HistoryEntry
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.components.MarkdownViewer
import com.jacquotq.lekker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class CollectionTab {
    HISTORY, FAVORITES
}

@Composable
fun CollectionScreen(storage: AppStorage, tts: TTSService, settings: AppSettings) {
    var selectedTab by remember { mutableStateOf(CollectionTab.HISTORY) }
    var history by remember { mutableStateOf(storage.loadHistory()) }
    var favorites by remember { mutableStateOf(storage.loadFavorites()) }
    var showClearDialog by remember { mutableStateOf(false) }
    val isZh = settings.language == "zh"

    val bgColor    = MaterialTheme.colorScheme.background
    val borderColor = MaterialTheme.colorScheme.outline
    val textColor  = MaterialTheme.colorScheme.onSurface
    val hintColor  = MaterialTheme.colorScheme.onSurfaceVariant

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isZh) "清空历史" else "Clear History") },
            text = {
                Text(
                    if (isZh) "确定要清空所有历史记录吗？此操作不可撤销。"
                    else "Are you sure you want to clear all history? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    storage.saveHistory(mutableListOf())
                    history = mutableListOf()
                    showClearDialog = false
                }) { Text(if (isZh) "清空" else "Clear", color = NotionAccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (isZh) "取消" else "Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Page header ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (isZh) "我的单词库" else "My Collection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            if (selectedTab == CollectionTab.HISTORY && history.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text(
                        if (isZh) "清空" else "Clear all",
                        fontSize = 13.sp,
                        color = hintColor
                    )
                }
            }
        }

        HorizontalDivider(color = borderColor)

        // ── Tab Bar ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedTab = tab }
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (tab) {
                                CollectionTab.HISTORY -> if (isZh) "历史记录" else "History"
                                CollectionTab.FAVORITES -> if (isZh) "我的收藏" else "Favorites"
                            },
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) textColor else hintColor
                        )
                        Text(
                            text = when (tab) {
                                CollectionTab.HISTORY -> history.size.toString()
                                CollectionTab.FAVORITES -> favorites.size.toString()
                            },
                            fontSize = 11.sp,
                            color = if (isSelected) hintColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Content ──────────────────────────────────────
        when (selectedTab) {
            CollectionTab.HISTORY -> {
                if (history.isEmpty()) {
                    EmptyState(
                        isZh = isZh,
                        emoji = "📭",
                        title = if (isZh) "暂无历史记录" else "No history yet",
                        subtitle = if (isZh) "搜索荷兰语单词后将显示在此处" else "Search results will appear here."
                    )
                } else {
                    val grouped = remember(history) { groupByDate(history, isZh) }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                        grouped.forEach { (label, entries) ->
                            item(key = "header_$label") {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hintColor,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(entries, key = { it.id }) { entry ->
                                HistoryRow(
                                    entry    = entry,
                                    tts      = tts,
                                    borderColor = borderColor,
                                    hintColor   = hintColor,
                                    textColor   = textColor,
                                    onDelete = {
                                        val updated = history.toMutableList().also { l -> l.remove(entry) }
                                        storage.saveHistory(updated)
                                        history = updated
                                    }
                                )
                                HorizontalDivider(color = borderColor)
                            }
                        }
                    }
                }
            }
            CollectionTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    EmptyState(
                        isZh = isZh,
                        emoji = "⭐",
                        title = if (isZh) "暂无收藏" else "No favorites yet",
                        subtitle = if (isZh) "在查询结果中点击 ★ 收藏单词" else "Tap ★ in search results to save words."
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                        items(favorites, key = { it.id }) { entry ->
                            FavoriteRow(
                                entry       = entry,
                                tts         = tts,
                                isZh        = isZh,
                                borderColor = borderColor,
                                textColor   = textColor,
                                hintColor   = hintColor,
                                onDelete    = {
                                    val favs = favorites.toMutableList().also { it.remove(entry) }
                                    storage.saveFavorites(favs)
                                    favorites = favs
                                }
                            )
                            HorizontalDivider(color = borderColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    isZh: Boolean,
    emoji: String,
    title: String,
    subtitle: String
) {
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 15.sp,
                color = hintColor
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = hintColor
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    tts: TTSService,
    borderColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.query,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sdf.format(Date(entry.timestamp)),
                        fontSize = 12.sp,
                        color = hintColor
                    )
                    NotionTag("cached", TagGreenBg, TagGreenText)
                }
            }
            Text(
                if (expanded) "▾" else "▸",
                fontSize = 14.sp,
                color = hintColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Text("×", fontSize = 18.sp, color = hintColor)
            }
        }

        if (!expanded) {
            val preview = entry.response
                .replace(Regex("#+\\s"), "")
                .replace(Regex("[*_>`|]"), "")
                .trim()
                .take(80)
            Text(
                text = preview + if (entry.response.length > 80) "..." else "",
                fontSize = 13.sp,
                color = hintColor,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(8.dp))
                MarkdownViewer(markdown = entry.response, tts = tts)
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    entry: FavoriteEntry,
    tts: TTSService,
    isZh: Boolean,
    borderColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val daysUntil = ((entry.nextReview - System.currentTimeMillis()) / 86_400_000).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.query,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        daysUntil <= 0 -> NotionTag(
                            if (isZh) "今天复习" else "due today",
                            TagOrangeBg, TagOrangeText
                        )
                        daysUntil == 1 -> NotionTag(
                            if (isZh) "明天复习" else "tomorrow",
                            TagBlueBg, TagBlueText
                        )
                        else -> NotionTag(
                            if (isZh) "${daysUntil}天后" else "in ${daysUntil}d",
                            TagGreenBg, TagGreenText
                        )
                    }
                    Text(
                        if (isZh) "间隔 ${entry.interval}天" else "${entry.interval}d interval",
                        fontSize = 11.sp,
                        color = hintColor
                    )
                }
            }

            Text(
                if (expanded) "▾" else "▸",
                fontSize = 14.sp,
                color = hintColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Text("×", fontSize = 18.sp, color = hintColor)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = borderColor)
                Spacer(Modifier.height(8.dp))
                MarkdownViewer(markdown = entry.response, tts = tts)
            }
        }
    }
}

private fun groupByDate(
    history: List<HistoryEntry>,
    isZh: Boolean
): List<Pair<String, List<HistoryEntry>>> {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year  = cal.get(Calendar.YEAR)
    val sdf   = SimpleDateFormat("MM/dd", Locale.getDefault())

    val groups = mutableMapOf<String, MutableList<HistoryEntry>>()
    for (entry in history) {
        cal.timeInMillis = entry.timestamp
        val entryDay  = cal.get(Calendar.DAY_OF_YEAR)
        val entryYear = cal.get(Calendar.YEAR)

        val label = when {
            entryYear == year && entryDay == today ->
                if (isZh) "今天" else "TODAY"
            entryYear == year && entryDay == today - 1 ->
                if (isZh) "昨天" else "YESTERDAY"
            else -> sdf.format(Date(entry.timestamp))
        }
        groups.getOrPut(label) { mutableListOf() }.add(entry)
    }
    return groups.toList()
}
