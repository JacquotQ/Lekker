package com.jacquotq.lekker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Lekker palette (Peach / warm) ────────────────────────────────
val NotionText          = Color(0xFF37352F)
val NotionTextSecondary = Color(0xFFA4A3AC)
val NotionTextTertiary  = Color(0xFFA4A3AC)
val NotionBg            = Color(0xFFFFEAD8)      // warm peach
val NotionBgSecondary   = Color(0xFFFFFFFF)      // white cards / surfaces
val NotionBorder        = Color(0xFFE8D0BA)      // peach-tinted border
val NotionAccentBlue    = Color(0xFF337EA9)
val NotionAccentRed     = Color(0xFFEB5757)
val NotionAccentOrange  = Color(0xFFD9730D)
val NotionAccentGreen   = Color(0xFF448361)

// Peach Dark palette
val NotionTextDark      = Color(0xFFECEAE0)      // light text for dark bg
val NotionBgDark        = Color(0xFF2A2520)      // dark warm background
val NotionBgSecondaryDark = Color(0xFF3E3935)   // dark card background
val NotionBorderDark    = Color(0xFF5C5248)     // dark peach-tinted border

// Tag background / text pairs
val TagOrangeBg   = Color(0xFFFDECC8)
val TagOrangeText = Color(0xFF9A6700)
val TagGreenBg    = Color(0xFFDBEDDB)
val TagGreenText  = Color(0xFF1A7A2E)
val TagBlueBg     = Color(0xFFD3E5EF)
val TagBlueText   = Color(0xFF24548D)
val TagRedBg      = Color(0xFFFFE2DD)
val TagRedText    = Color(0xFF93000A)

// Tag dark variants
val TagOrangeBgDark   = Color(0xFF5C4620)
val TagOrangeTextDark = Color(0xFFDBA828)
val TagGreenBgDark    = Color(0xFF2D5540)
val TagGreenTextDark  = Color(0xFF5FD181)
val TagBlueBgDark     = Color(0xFF2D4666)
val TagBlueTextDark   = Color(0xFF7FB3E5)
val TagRedBgDark      = Color(0xFF5C2C2C)
val TagRedTextDark    = Color(0xFFF08080)

// ── B&W palette ───────────────────────────────────────────────────
val BwText          = Color(0xFF1A1A1A)
val BwTextSecondary = Color(0xFF888888)
val BwBg            = Color(0xFFF5F5F5)
val BwBgSecondary   = Color(0xFFFFFFFF)
val BwBorder        = Color(0xFFBCBCBC)      // darker gray border for B&W

// B&W Dark palette
val BwTextDark      = Color(0xFFEAEAEA)
val BwTextSecondaryDark = Color(0xFF999999)
val BwBgDark        = Color(0xFF1A1A1A)
val BwBgSecondaryDark   = Color(0xFF2E2E2E)
val BwBorderDark    = Color(0xFF444444)

// ── Color schemes ────────────────────────────────────────────────
// 桃 / Peach (Light)
private val PeachColorScheme = lightColorScheme(
    primary            = NotionText,
    onPrimary          = Color.White,
    primaryContainer   = NotionBgSecondary,
    secondary          = NotionAccentBlue,
    secondaryContainer = TagBlueBg,
    background         = NotionBg,
    surface            = NotionBgSecondary,
    surfaceVariant     = NotionBgSecondary,
    outline            = NotionBorder,
    outlineVariant     = NotionBorder,
    onBackground       = NotionText,
    onSurface          = NotionText,
    onSurfaceVariant   = NotionTextSecondary,
    error              = NotionAccentRed,
    errorContainer     = TagRedBg,
    onErrorContainer   = TagRedText,
)

// 暗桃 / Dark Peach
private val PeachDarkColorScheme = darkColorScheme(
    primary            = NotionTextDark,
    onPrimary          = NotionBgDark,
    primaryContainer   = NotionBgSecondaryDark,
    secondary          = Color(0xFF7FB3E5),
    secondaryContainer = TagBlueBgDark,
    background         = NotionBgDark,
    surface            = NotionBgSecondaryDark,
    surfaceVariant     = NotionBgSecondaryDark,
    outline            = NotionBorderDark,
    outlineVariant     = NotionBorderDark,
    onBackground       = NotionTextDark,
    onSurface          = NotionTextDark,
    onSurfaceVariant   = Color(0xFFC4C3CC),
    error              = Color(0xFFFF8A8A),
    errorContainer     = TagRedBgDark,
    onErrorContainer   = TagRedTextDark,
)

// 白 / White (Light B&W)
private val BwColorScheme = lightColorScheme(
    primary            = BwText,
    onPrimary          = Color.White,
    primaryContainer   = BwBgSecondary,
    secondary          = Color(0xFF555555),
    secondaryContainer = Color(0xFFE8E8E8),
    background         = BwBg,
    surface            = BwBgSecondary,
    surfaceVariant     = BwBgSecondary,
    outline            = BwBorder,
    outlineVariant     = BwBorder,
    onBackground       = BwText,
    onSurface          = BwText,
    onSurfaceVariant   = BwTextSecondary,
    error              = Color(0xFFB00020),
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF93000A),
)

// 黑 / Black (Dark B&W)
private val BwDarkColorScheme = darkColorScheme(
    primary            = BwTextDark,
    onPrimary          = BwBgDark,
    primaryContainer   = BwBgSecondaryDark,
    secondary          = Color(0xFFAAAAAA),
    secondaryContainer = Color(0xFF424242),
    background         = BwBgDark,
    surface            = BwBgSecondaryDark,
    surfaceVariant     = BwBgSecondaryDark,
    outline            = BwBorderDark,
    outlineVariant     = BwBorderDark,
    onBackground       = BwTextDark,
    onSurface          = BwTextDark,
    onSurfaceVariant   = BwTextSecondaryDark,
    error              = Color(0xFFFF8A8A),
    errorContainer     = Color(0xFF5C2C2C),
    onErrorContainer   = Color(0xFFF08080),
)

// ── Theme entry point ─────────────────────────────────────────────
@Composable
fun LekkerTheme(
    theme: String = "peach",          // "peach" | "peach_dark" | "bw" | "bw_dark"
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        "bw"        -> BwColorScheme          // 白 / White
        "bw_dark"   -> BwDarkColorScheme      // 黑 / Black
        "peach_dark"-> PeachDarkColorScheme   // 暗桃 / Dark Peach
        else        -> PeachColorScheme       // 桃 / Peach (default)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
