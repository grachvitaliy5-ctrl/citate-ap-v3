package com.example.goals

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Дизайн-токены. Контраст проверен по WCAG 2.1:
 * текст ≥ 4.5:1 к фону, статусные клетки ≥ 3:1 к «туману».
 */
@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val fog: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val green: Color,
    val yellow: Color,
    val red: Color
)

val LightColors = AppColors(
    bg = Color(0xFFF6F6F3),
    surface = Color(0xFFFFFFFF),
    fog = Color(0xFFE6E6E1),
    line = Color(0xFFDADAD4),
    text = Color(0xFF151515),
    muted = Color(0xFF66665F),
    accent = Color(0xFF2C4A6E),
    onAccent = Color(0xFFF6F6F3),
    green = Color(0xFF2E7D4F),
    yellow = Color(0xFFA97A00),
    red = Color(0xFFB83A2E)
)

val DarkColors = AppColors(
    bg = Color(0xFF0C0C0C),
    surface = Color(0xFF151515),
    fog = Color(0xFF1D1D1D),
    line = Color(0xFF2A2A2A),
    text = Color(0xFFEDEDE8),
    muted = Color(0xFF9B9B95),
    accent = Color(0xFF9DB4D3),
    onAccent = Color(0xFF0C0C0C),
    green = Color(0xFF4FB57E),
    yellow = Color(0xFFD9A92E),
    red = Color(0xFFE0685B)
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** Сетка 4/8 dp */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}

val AppShape = RoundedCornerShape(10.dp)

object Type {
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val heading = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val quote = TextStyle(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.fog, onSurfaceVariant = c.muted,
            outline = c.line, error = c.red
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.fog, onSurfaceVariant = c.muted,
            outline = c.line, error = c.red
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
