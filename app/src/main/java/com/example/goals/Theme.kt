package com.example.goals

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Палитра сцены «Путь»: ночное небо, лакированное дерево, бумага, золото, сакура.
 * Все экраны приложения используют её, чтобы выглядеть как одно целое со сценой.
 */
@Immutable
data class AppColors(
    val bg: Color,        // ночное небо
    val surface: Color,   // дерево
    val fog: Color,       // светлое дерево (кнопки, ячейки)
    val line: Color,      // коричневая рамка
    val text: Color,      // кремовый текст
    val muted: Color,     // приглушённый текст
    val accent: Color,    // золото
    val onAccent: Color,  // тушь на золоте
    val green: Color,
    val yellow: Color,
    val red: Color,
    val paper: Color,     // бумажная плитка
    val paperShade: Color,
    val ink: Color,       // тушь
    val sakura: Color,
    val seal: Color,      // красная печать
    val outline: Color    // самая тёмная обводка пикселей
)

val PathColors = AppColors(
    bg = Color(0xFF0B0A1F),
    surface = Color(0xFF21140F),
    fog = Color(0xFF2C1B14),
    line = Color(0xFF6A4A30),
    text = Color(0xFFF6ECD9),
    muted = Color(0xFFB9A58A),
    accent = Color(0xFFD7B06A),
    onAccent = Color(0xFF2A1A12),
    green = Color(0xFF5FBF5A),
    yellow = Color(0xFFF2C14E),
    red = Color(0xFFE0453A),
    paper = Color(0xFFECDCB4),
    paperShade = Color(0xFFC6B083),
    ink = Color(0xFF2A1A12),
    sakura = Color(0xFFFF8FB8),
    seal = Color(0xFFC0392B),
    outline = Color(0xFF120A07)
)

val LocalAppColors = staticCompositionLocalOf { PathColors }

/** Сетка 4/8 dp */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
}

/** Пиксельный стиль: углы прямые. */
val AppShape = RoundedCornerShape(0.dp)

val PixFont = FontFamily(Font(R.font.pressstart))
val BodyFont = FontFamily(Font(R.font.pixelify))

object Type {
    val title = TextStyle(fontFamily = PixFont, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 1.5.sp)
    val heading = TextStyle(fontFamily = BodyFont, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontFamily = BodyFont, fontSize = 18.sp, lineHeight = 24.sp)
    val quote = TextStyle(fontFamily = BodyFont, fontSize = 20.sp, lineHeight = 27.sp)
    val caption = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, lineHeight = 20.sp)
    val label = TextStyle(fontFamily = PixFont, fontSize = 8.sp, lineHeight = 13.sp, letterSpacing = 1.sp)
    val button = TextStyle(fontFamily = PixFont, fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 1.sp)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val c = PathColors
    val scheme = darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent,
        background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text,
        surfaceVariant = c.fog, onSurfaceVariant = c.muted,
        outline = c.line, error = c.red
    )
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
