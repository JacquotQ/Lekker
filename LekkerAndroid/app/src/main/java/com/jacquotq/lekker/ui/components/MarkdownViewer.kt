package com.jacquotq.lekker.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacquotq.lekker.service.TTSService
import com.jacquotq.lekker.ui.theme.*

@Composable
fun MarkdownViewer(
    markdown: String,
    tts: TTSService,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()
    Column(modifier = modifier.fillMaxWidth()) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                // H2 — only show IPA pronunciation; TTS buttons live in the SearchScreen header
                line.startsWith("## ") -> {
                    val text = line.removePrefix("## ")
                    val ipaMatch = Regex("""/(.+?)/""").find(text)
                    val ipa = ipaMatch?.value ?: ""  // e.g. "/ˈlɛ.kər/"
                    if (ipa.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = ipa,
                            fontSize = 16.sp,
                            color = NotionTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    i++
                }
                // H3
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NotionText,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                    i++
                }
                // Blockquote — Notion style (left border, no TTS — it's a summary, not Dutch)
                line.startsWith("> ") -> {
                    val text = line.removePrefix("> ")
                    val borderColor = NotionText
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = NotionTextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                    i++
                }
                // Horizontal rule
                line.trim() == "---" || line.trim() == "***" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = NotionBorder
                    )
                    i++
                }
                // Table — Notion style
                line.trimStart().startsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    MarkdownTable(tableLines, tts)
                }
                // Bullet list
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val bullet = line.trimStart().removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                        Text(
                            "•  ",
                            color = NotionTextTertiary,
                            fontSize = 15.sp
                        )
                        InlineMarkdown(bullet, Modifier.weight(1f))
                    }
                    i++
                }
                // Numbered list
                line.trimStart().matches(Regex("""\d+\. .*""")) -> {
                    val num = line.trimStart().substringBefore(".").trim()
                    val rest = line.trimStart().substringAfter(". ")
                    Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                        Text(
                            "$num.  ",
                            color = NotionTextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        InlineMarkdown(rest, Modifier.weight(1f))
                    }
                    i++
                }
                // Empty line
                line.isBlank() -> { Spacer(Modifier.height(6.dp)); i++ }
                // Normal paragraph
                else -> {
                    InlineMarkdown(line, Modifier.padding(vertical = 3.dp))
                    i++
                }
            }
        }
    }
}

@Composable
fun MarkdownTable(lines: List<String>, tts: TTSService) {
    val rows = lines.filter { !it.contains("---") }
        .map { row -> row.trim().trim('|').split("|").map { it.trim() } }
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, NotionBorder, RoundedCornerShape(4.dp))
    ) {
        rows.forEachIndexed { rowIdx, cells ->
            val isHeader = rowIdx == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHeader) NotionBgSecondary
                        else Color.Transparent
                    )
            ) {
                cells.forEachIndexed { cellIdx, cell ->
                    // Strip bold/italic markdown, then catch any remaining stray *
                    val clean = cell
                        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
                        .replace(Regex("""\*(.+?)\*"""), "$1")
                        .replace("*", "")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (isHeader) {
                            Text(
                                clean,
                                color = NotionTextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    clean,
                                    fontSize = 14.sp,
                                    color = NotionText,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (cellIdx == 0 && clean.isNotBlank()) {
                                    SpeakButton(clean, tts, iconSize = 14.dp)
                                }
                            }
                        }
                    }
                }
            }
            if (rowIdx < rows.size - 1) {
                HorizontalDivider(color = NotionBorder, thickness = 1.dp)
            }
        }
    }
}

/** Strip markdown formatting so TTS doesn't read out *, **, _ etc. */
private fun stripMarkdown(text: String): String = text
    .replace("**", "")
    .replace("*", "")
    .replace("_", "")
    .replace("~", "")
    .trim()

@Composable
fun SpeakButton(
    text: String,
    tts: TTSService,
    slow: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    val cleanText = stripMarkdown(text)
    val speaking by tts.speaking.collectAsState()
    val isActive = tts.isSpeaking(cleanText)

    IconButton(
        onClick = { tts.toggle(cleanText, slow) },
        modifier = Modifier.size(iconSize + 16.dp)
    ) {
        if (slow) {
            Text(
                if (isActive && speaking) "⏹" else "🐢",
                fontSize = (iconSize.value * 0.85f).sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Speak",
                tint = if (isActive && speaking) NotionAccentBlue
                       else NotionTextTertiary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun InlineMarkdown(text: String, modifier: Modifier = Modifier) {
    val annotated = buildAnnotatedString {
        val boldRegex = Regex("""\*\*(.+?)\*\*""")
        val italicRegex = Regex("""_(.+?)_""")
        var lastEnd = 0
        val matches = (boldRegex.findAll(text) + italicRegex.findAll(text))
            .sortedBy { it.range.first }
        for (match in matches) {
            if (match.range.first > lastEnd) append(text.substring(lastEnd, match.range.first))
            if (match.value.startsWith("**")) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = NotionText)) {
                    append(match.groupValues[1])
                }
            } else {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = NotionTextSecondary)) {
                    append(match.groupValues[1])
                }
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) {
            // Strip any leftover stray markdown markers
            append(text.substring(lastEnd).replace("**", "").replace("*", ""))
        }
    }
    Text(
        text = annotated,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        color = NotionText,
        modifier = modifier
    )
}
