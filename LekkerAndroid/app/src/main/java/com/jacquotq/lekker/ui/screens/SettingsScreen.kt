package com.jacquotq.lekker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.jacquotq.lekker.data.UserProfile
import com.jacquotq.lekker.ui.theme.*

@Composable
fun SettingsScreen(
    storage: AppStorage,
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit
) {
    val initProfile = remember { storage.loadProfile() }

    // Local mutable copies — persisted via onSettingsChanged
    var useCache by remember { mutableStateOf(settings.useCache) }
    var language by remember { mutableStateOf(settings.language) }
    var theme    by remember { mutableStateOf(settings.theme) }

    var etymologyW    by remember { mutableFloatStateOf(initProfile.etymologyWeight.toFloat()) }
    var storiesW      by remember { mutableFloatStateOf(initProfile.storiesWeight.toFloat()) }
    var connectionsW  by remember { mutableFloatStateOf(initProfile.connectionsWeight.toFloat()) }
    var formalW       by remember { mutableFloatStateOf(initProfile.formalWeight.toFloat()) }

    var showResetDialog by remember { mutableStateOf(false) }
    val isZh = language == "zh"

    // Theme-aware colours
    val bgColor      = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val borderColor  = MaterialTheme.colorScheme.outline
    val textColor    = MaterialTheme.colorScheme.onSurface
    val hintColor    = MaterialTheme.colorScheme.onSurfaceVariant

    fun pushSettings() {
        onSettingsChanged(AppSettings(useCache = useCache, language = language, theme = theme))
    }

    // Also save profile on dispose (sliders)
    DisposableEffect(Unit) {
        onDispose {
            storage.saveProfile(UserProfile(
                etymologyWeight   = etymologyW.toDouble(),
                storiesWeight     = storiesW.toDouble(),
                connectionsWeight = connectionsW.toDouble(),
                formalWeight      = formalW.toDouble()
            ))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (isZh) "重置学习偏好" else "Reset Learning Preferences") },
            text = {
                Text(
                    if (isZh) "将所有权重恢复为默认值 (1.0)？"
                    else "Reset all weights to default (1.0)?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    storage.resetProfile()
                    etymologyW = 1f; storiesW = 1f; connectionsW = 1f; formalW = 1f
                    showResetDialog = false
                }) { Text(if (isZh) "确认" else "Confirm", color = NotionAccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("⚙️", fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isZh) "设置" else "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        HorizontalDivider(color = borderColor)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Language ─────────────────────────────────
            SettingsSection(
                title = if (isZh) "语言" else "LANGUAGE",
                surfaceColor = surfaceColor,
                hintColor = hintColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (isZh) "应用语言" else "App Language",
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Text(
                            if (isZh) "切换界面和 AI 回答语言"
                            else "Switch UI and AI response language",
                            fontSize = 12.sp,
                            color = hintColor
                        )
                    }
                    LanguageToggle(
                        selected = language,
                        borderColor = borderColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        hintColor = hintColor,
                        onSelect = { language = it; pushSettings() }
                    )
                }
            }

            // ── Theme ────────────────────────────────────
            SettingsSection(
                title = if (isZh) "主题" else "THEME",
                surfaceColor = surfaceColor,
                hintColor = hintColor
            ) {
                Column {
                    Text(
                        if (isZh) "配色方案" else "Color Scheme",
                        fontSize = 15.sp,
                        color = textColor
                    )
                    Text(
                        if (isZh) "暖桃色 或 黑白简约" else "Warm peach or black & white",
                        fontSize = 12.sp,
                        color = hintColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ThemeToggle(
                        selected = theme,
                        isZh = isZh,
                        borderColor = borderColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        hintColor = hintColor,
                        onSelect = { theme = it; pushSettings() }
                    )
                }
            }

            // ── Cache ────────────────────────────────────
            SettingsSection(
                title = if (isZh) "缓存" else "CACHE",
                surfaceColor = surfaceColor,
                hintColor = hintColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isZh) "缓存结果" else "Cache Results",
                            fontSize = 15.sp,
                            color = textColor
                        )
                        Text(
                            if (isZh) "相同查询直接读取本地缓存"
                            else "Load identical queries from local cache",
                            fontSize = 12.sp,
                            color = hintColor
                        )
                    }
                    Switch(
                        checked = useCache,
                        onCheckedChange = { useCache = it; pushSettings() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = MaterialTheme.colorScheme.surface,
                            checkedTrackColor   = NotionAccentOrange,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedTrackColor = hintColor
                        )
                    )
                }
            }

            // ── Learning preferences ─────────────────────
            SettingsSection(
                title = if (isZh) "学习偏好" else "LEARNING",
                surfaceColor = surfaceColor,
                hintColor = hintColor
            ) {
                Text(
                    if (isZh) "调整 AI 回答的侧重点 (0 = 不显示, 1 = 默认, 2 = 加强)"
                    else "Adjust AI response emphasis (0 = hide, 1 = default, 2 = emphasize)",
                    fontSize = 12.sp,
                    color = hintColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WeightSlider(if (isZh) "🧩 词根记忆" else "🧩 Etymology", etymologyW, textColor) {
                    etymologyW = it
                    storage.saveProfile(UserProfile(etymologyW.toDouble(), storiesW.toDouble(), connectionsW.toDouble(), formalW.toDouble()))
                }
                WeightSlider(if (isZh) "📚 文化故事" else "📚 Cultural Stories", storiesW, textColor) {
                    storiesW = it
                    storage.saveProfile(UserProfile(etymologyW.toDouble(), storiesW.toDouble(), connectionsW.toDouble(), formalW.toDouble()))
                }
                WeightSlider(if (isZh) "🔗 语言联系" else "🔗 Language Connections", connectionsW, textColor) {
                    connectionsW = it
                    storage.saveProfile(UserProfile(etymologyW.toDouble(), storiesW.toDouble(), connectionsW.toDouble(), formalW.toDouble()))
                }
                WeightSlider(if (isZh) "📝 正式用法" else "📝 Formal Usage", formalW, textColor) {
                    formalW = it
                    storage.saveProfile(UserProfile(etymologyW.toDouble(), storiesW.toDouble(), connectionsW.toDouble(), formalW.toDouble()))
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = hintColor)
                ) {
                    Text(if (isZh) "重置偏好" else "Reset Preferences", fontSize = 13.sp)
                }
            }

            // ── About ────────────────────────────────────
            SettingsSection(
                title = if (isZh) "关于" else "ABOUT",
                surfaceColor = surfaceColor,
                hintColor = hintColor
            ) {
                Text("Lekker 🇳🇱", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isZh) "荷兰语学习助手" else "Dutch Learning Assistant",
                    fontSize = 14.sp,
                    color = hintColor
                )
                Text("v2.0.0", fontSize = 13.sp, color = hintColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    "CC BY-NC-SA 4.0 © 2025 JacquotQ",
                    fontSize = 12.sp,
                    color = hintColor
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable sub-composables ─────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    surfaceColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = hintColor,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(surfaceColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun WeightSlider(
    label: String,
    value: Float,
    textColor: androidx.compose.ui.graphics.Color,
    onFinished: (Float) -> Unit
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = textColor)
            Text(
                String.format("%.1f", local),
                fontSize = 14.sp,
                color = NotionAccentOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onFinished(local) },
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor        = NotionAccentOrange,
                activeTrackColor  = NotionAccentOrange,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun LanguageToggle(
    selected: String,
    borderColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    val options = listOf("中文" to "zh", "English" to "en")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(borderColor)
            .padding(2.dp)
    ) {
        options.forEach { (label, code) ->
            val isSelected = selected == code
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) surfaceColor else borderColor)
                    .clickable { onSelect(code) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) textColor else hintColor
                )
            }
        }
    }
}

@Composable
private fun ThemeToggle(
    selected: String,
    isZh: Boolean,
    borderColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        (if (isZh) "🍑 桃" else "🍑 Peach") to "peach",
        (if (isZh) "🌙 暗桃" else "🌙 Dark Peach") to "peach_dark",
        (if (isZh) "⬜ 白" else "⬜ White") to "bw",
        (if (isZh) "⬛ 黑" else "⬛ Black") to "bw_dark"
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(borderColor)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.take(2).forEach { (label, code) ->
                val isSelected = selected == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) surfaceColor else borderColor)
                        .clickable { onSelect(code) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) textColor else hintColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(borderColor)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.drop(2).forEach { (label, code) ->
                val isSelected = selected == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) surfaceColor else borderColor)
                        .clickable { onSelect(code) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) textColor else hintColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
