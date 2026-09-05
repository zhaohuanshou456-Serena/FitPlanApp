package com.fitplan.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * 活力翠绿 · 多彩活泼配色
 * 主色：青/翠绿（健身、清新）  辅色：青蓝、紫罗兰（模块/趋势强调）
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF0FA958),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCF2D1),
    onPrimaryContainer = Color(0xFF053B22),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = Color(0xFF7B4DFF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DEFF),
    onTertiaryContainer = Color(0xFF26005E),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADC),
    onErrorContainer = Color(0xFF5C1111),
    background = Color(0xFFF5FAF6),
    onBackground = Color(0xFF171B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171B18),
    surfaceVariant = Color(0xFFE1ECE4),
    onSurfaceVariant = Color(0xFF454F48),
    outline = Color(0xFF768078),
    outlineVariant = Color(0xFFC5D0C7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF66E59A),
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF0A5C33),
    onPrimaryContainer = Color(0xFFBCF2D1),
    secondary = Color(0xFF6FD7E0),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF004F55),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFC9B8FF),
    onTertiary = Color(0xFF2B0060),
    tertiaryContainer = Color(0xFF4F2790),
    onTertiaryContainer = Color(0xFFE9DEFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1511),
    onBackground = Color(0xFFDDE6DE),
    surface = Color(0xFF141C17),
    onSurface = Color(0xFFDDE6DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404943)
)

@Composable
fun FitPlanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 固定活力翠绿品牌色；如需壁纸取色可改 true
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
