package com.fitplan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * 黑金运动风 · 私人训练会所
 * 哑光黑底 + 深灰分区（比纯黑亮一点，护眼）+ 暗金点缀（只做强调）
 * 白主字/灰副字，数字用细等宽字体（数字感强、专业）
 */

private val Gold = Color(0xFFC9A24B)            // 暗金（强调）
private val GoldContainer = Color(0xFF3A2F16)
private val GoldOnContainer = Color(0xFFE8D5A0)
private val Background = Color(0xFF0F1115)      // 深黑，非纯黑，护眼
private val Surface = Color(0xFF15181D)         // 深灰分区
private val Surface2 = Color(0xFF1E2229)
private val GraySec = Color(0xFF5A5F66)         // 中性灰（多数控件走这里，避免金色泛滥）
private val GrayText = Color(0xFF9AA0A8)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldOnContainer,
    secondary = GraySec,
    onSecondary = Color(0xFFECEDEF),
    secondaryContainer = Surface2,
    onSecondaryContainer = Color(0xFFE6E8EA),
    tertiary = Color(0xFF8CA0B8),
    onTertiary = Color(0xFF12161C),
    tertiaryContainer = Color(0xFF1E2429),
    onTertiaryContainer = Color(0xFFC7D3E0),
    error = Color(0xFFE5484D),
    onError = Color.White,
    background = Background,
    onBackground = Color(0xFFF2F3F5),
    surface = Surface,
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Surface2,
    onSurfaceVariant = GrayText,
    outline = Color(0xFF3A3F45),
    outlineVariant = Color(0xFF2A2E34)
)

// 数字用细等宽字体（中文仍用系统字体，仅数字/拉丁更“专业”）
private val Mono = FontFamily.Monospace

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Light, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Light, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp)
)

@Composable
fun FitPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
