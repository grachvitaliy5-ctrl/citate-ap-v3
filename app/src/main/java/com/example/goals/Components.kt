package com.example.goals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.sin

// ---------- Пиксельные поверхности ----------

/** Панель как в сцене: тёмная обводка, золотая рамка, заливка и тень со смещением. */
fun Modifier.pixelPanel(
    fill: Color,
    edge: Color,
    outline: Color = PathColors.outline,
    shadow: Dp = 4.dp,
    shade: Color? = null
): Modifier = this.drawBehind {
    val s = shadow.toPx()
    val o = 2.dp.toPx()
    if (s > 0f) drawRect(Color(0x8C000000), topLeft = Offset(s, s), size = size)
    drawRect(outline, size = size)
    drawRect(edge, topLeft = Offset(o, o), size = Size(size.width - 2 * o, size.height - 2 * o))
    drawRect(fill, topLeft = Offset(2 * o, 2 * o), size = Size(size.width - 4 * o, size.height - 4 * o))
    if (shade != null) {
        val w = 3.dp.toPx()
        drawRect(shade, topLeft = Offset(2 * o, size.height - 2 * o - w), size = Size(size.width - 4 * o, w))
        drawRect(shade, topLeft = Offset(size.width - 2 * o - w, 2 * o), size = Size(w, size.height - 4 * o))
    }
}

/** Деревянная табличка с золотой рамкой. */
@Composable
fun WoodPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalAppColors.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.s)
            .pixelPanel(c.surface, c.accent)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        content = content
    )
}

/** Бумажная плитка с тушью; strip — цветная полоска снизу, как у кнопок рутин в сцене. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    strip: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    var m = modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = 6.dp)
        .pixelPanel(c.paper, c.outline, shadow = 3.dp, shade = c.paperShade)
    if (strip != null) {
        m = m.drawBehind {
            val o = 4.dp.toPx()
            drawRect(strip, topLeft = Offset(o, size.height - o - 4.dp.toPx()), size = Size(size.width - 2 * o, 4.dp.toPx()))
        }
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 14.dp), content = content)
}

/** Ночное небо со звёздами, горой и падающими лепестками — фон всех экранов кроме сцены. */
@Composable
fun SceneBackdrop(modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val t by rememberInfiniteTransition(label = "sky").animateFloat(
        0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart), label = "t"
    )
    val stars = remember {
        val r = java.util.Random(7)
        List(70) { Triple(r.nextFloat(), r.nextFloat() * 0.8f, r.nextFloat()) }
    }
    val petals = remember {
        val r = java.util.Random(11)
        List(14) { Triple(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f) }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0B0A1F), Color(0xFF150F33), Color(0xFF241542))))
        val px = 3.dp.toPx()
        stars.forEach { (x, y, k) ->
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin((t * 6.283f * 3f) + k * 20f))
            drawRect(Color.White.copy(alpha = tw * (0.35f + 0.5f * k)), topLeft = Offset(x * size.width, y * size.height), size = Size(px * (if (k > 0.85f) 1f else 0.67f), px * (if (k > 0.85f) 1f else 0.67f)))
        }
        // гора с тёмным духом вдали
        val step = 6.dp.toPx()
        val base = size.height
        var x = 0f
        while (x < size.width) {
            val u = x / size.width
            val h = (0.16f + 0.22f * maxOf(0f, 1f - kotlin.math.abs(u - 0.62f) * 2.6f) + 0.03f * sin(u * 40f)) * size.height
            drawRect(Color(0xFF1C1236), topLeft = Offset(x, base - h), size = Size(step + 1f, h))
            x += step
        }
        drawRect(c.seal.copy(alpha = 0.35f), topLeft = Offset(size.width * 0.62f - px, base - 0.38f * size.height + px * 4), size = Size(px * 2, px))
        // лепестки
        petals.forEach { (x0, y0, sp) ->
            val p = (y0 + t * sp * 2f) % 1f
            val xx = (x0 + 0.08f * sin(p * 12f + x0 * 9f)) * size.width
            drawRect(c.sakura.copy(alpha = 0.75f), topLeft = Offset(xx, p * size.height), size = Size(px * 2, px))
            drawRect(c.sakura.copy(alpha = 0.45f), topLeft = Offset(xx + px, p * size.height + px), size = Size(px, px))
        }
    }
}

private const val NOTE_PATH =
    "M44.6,60 A5.6,4.3 -18 1,0 55.8,56.6 A5.6,4.3 -18 1,0 44.6,60 Z M53.4,35 L56.2,35 L56.2,57.6 L53.4,57.6 Z M56.2,35 C61,36.8 65,41.2 63.8,47.6 C62,43.4 59.2,42.4 56.2,41.9 Z"

private val RAINBOW = listOf(
    Color(0xFFFF4D6D), Color(0xFFFFA53D), Color(0xFFFFE45C), Color(0xFF5DFC8D),
    Color(0xFF45D4FF), Color(0xFF6B7BFF), Color(0xFFD36BFF), Color(0xFFFF4D6D)
)

/** Знак приложения: лепесток с ореолом и нота, которая переливается всеми цветами. */
@Composable
fun AppMark(size: Dp = 44.dp) {
    val t by rememberInfiniteTransition(label = "note").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "hue"
    )
    val note = remember { PathParser().parsePathString(NOTE_PATH).toPath() }
    Box(Modifier.size(size)) {
        Image(painterResource(R.drawable.petal_mark), contentDescription = null, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val k = this.size.width / 108f
            val shift = t * 60f
            scale(k, k, pivot = Offset.Zero) {
                rotate(-12f, pivot = Offset(54f, 54f)) {
                    translate(0f, 0f) {
                        drawPath(note, Color.White.copy(alpha = 0.35f))
                        drawPath(
                            note,
                            Brush.linearGradient(
                                RAINBOW, start = Offset(30f + shift, 30f), end = Offset(60f + shift, 64f),
                                tileMode = TileMode.Repeated
                            )
                        )
                    }
                }
            }
        }
    }
}

// ---------- Базовые элементы ----------

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(start = Space.m, end = Space.m, top = Space.m, bottom = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppMark(46.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = Type.title, color = c.paper)
            if (subtitle != null) {
                Spacer(Modifier.height(Space.xs))
                Text(subtitle, style = Type.caption, color = c.muted)
            }
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(Space.s))
            PixelButton("+ " + actionLabel.uppercase(), onAction)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    val c = LocalAppColors.current
    Row(
        Modifier.padding(start = Space.m, end = Space.m, top = Space.l, bottom = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(c.sakura))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = Type.label, color = c.accent)
    }
}

@Composable
fun Line(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(Color(0xFF4A2E1C)))
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = LocalAppColors.current.accent) {
    Box(
        Modifier.heightIn(min = 48.dp).clickable(onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.s),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Type.body, color = color)
    }
}

/** Золотая пиксельная кнопка. */
@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalAppColors.current
    Box(
        modifier.heightIn(min = 44.dp)
            .pixelPanel(if (enabled) c.accent else c.fog, c.outline, shadow = 3.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Type.button, color = if (enabled) c.onAccent else c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun FlatButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    PixelButton(text.uppercase(), onClick, modifier.fillMaxWidth(), enabled)
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Box(
        modifier.heightIn(min = 44.dp)
            .pixelPanel(c.fog, c.line, shadow = 0.dp)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), style = Type.button, color = c.paper, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(text: String, actionLabel: String, onAction: () -> Unit) {
    val c = LocalAppColors.current
    PaperCard {
        Text(text, style = Type.body, color = c.ink)
        Spacer(Modifier.height(Space.s))
        Row {
            PixelButton("+ " + actionLabel.uppercase(), onAction)
        }
    }
}

/** Квадратный переключатель: золото — включено, тёмное дерево — выключено. */
@Composable
fun PixelSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Box(
        Modifier.size(width = 50.dp, height = 28.dp)
            .pixelPanel(if (checked) Color(0xFF6E5520) else c.bg, if (checked) c.accent else c.line, shadow = 0.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(6.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(16.dp).background(if (checked) c.accent else c.muted))
    }
}

@Composable
fun appFieldColors(): TextFieldColors {
    val c = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        focusedContainerColor = c.paper,
        unfocusedContainerColor = c.paper,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.outline,
        focusedLabelColor = c.ink,
        unfocusedLabelColor = Color(0xFF6A5440),
        cursorColor = c.ink
    )
}

@Composable
fun AppField(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, style = Type.caption) },
        singleLine = singleLine,
        shape = AppShape,
        colors = appFieldColors(),
        textStyle = Type.body,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) }
            .padding(horizontal = Space.s, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.body, color = c.text)
            if (subtitle != null) Text(subtitle, style = Type.caption, color = c.muted)
        }
        Spacer(Modifier.width(Space.s))
        PixelSwitch(checked, onChange)
    }
}

@Composable
fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = Type.body, color = c.text, modifier = Modifier.weight(1f))
        Text(value, style = Type.body, color = c.accent, textAlign = TextAlign.End)
    }
}

@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val sel = value == selected
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp)
                    .pixelPanel(if (sel) c.accent else Color(0xFF2C1B14), if (sel) c.outline else c.line, shadow = 0.dp)
                    .clickable { onSelect(value) }.padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label.uppercase(), style = Type.button, color = if (sel) c.ink else c.paper, textAlign = TextAlign.Center)
            }
        }
    }
}

// ---------- Навигация ----------

private val TAB_ICONS = listOf(
    // торii — Путь
    listOf("#########", ".#######.", "..#...#..", "#########", "..#...#..", "..#...#..", "..#...#..", "..#...#..", ".##...##."),
    // мишень — Цели
    listOf("...###...", ".##...##.", ".#..#..#.", "#..###..#", "#.##.##.#", "#..###..#", ".#..#..#.", ".##...##.", "...###..."),
    // свиток — Цитаты
    listOf(".#######.", "#.......#", ".#.###.#.", ".#.....#.", ".#.####..", ".#.....#.", ".#.##..#.", "#.......#", ".#######."),
    // календарь — Заметки
    listOf(".#.....#.", "#########", "#.......#", "#########", "#.#.#.#.#", "#########", "#.#.#.#.#", "#########", "........."),
    // фонарь — Ещё
    listOf("...###...", "..#####..", ".#.....#.", ".#.###.#.", ".#.###.#.", ".#.....#.", "..#####..", "...#.#...", "..#####..")
)

@Composable
private fun PixelIcon(rows: List<String>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val cell = size.width / 9f
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == '#') drawRect(color, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

@Composable
fun TabBar(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth()
            .drawBehind {
                drawRect(c.surface)
                drawRect(c.outline, size = Size(size.width, 2.dp.toPx()))
                drawRect(c.accent, topLeft = Offset(0f, 2.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
            }
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            titles.forEachIndexed { i, t ->
                val sel = i == selected
                val col = if (sel) c.accent else c.muted
                Box(
                    Modifier.weight(1f).heightIn(min = 60.dp).clickable { onSelect(i) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PixelIcon(TAB_ICONS[i % TAB_ICONS.size], col)
                        Spacer(Modifier.height(5.dp))
                        Text(t.uppercase(), color = col, style = Type.label.copy(fontSize = 7.sp), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(width = 14.dp, height = 3.dp).background(if (sel) c.sakura else Color.Transparent))
                    }
                }
            }
        }
    }
}


// ---------- Диалог ----------

@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val c = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .pixelPanel(c.surface, c.accent)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m)
        ) {
            Text(title.uppercase(), style = Type.title.copy(fontSize = Type.label.fontSize * 1.4f), color = c.accent)
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) { content() }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                GhostButton("Отмена", onDismiss, Modifier.weight(1f))
                PixelButton(confirmLabel.uppercase(), onConfirm, Modifier.weight(1f))
            }
        }
    }
}

/** Удаление в два нажатия, чтобы не удалить случайно. */
@Composable
fun DeleteAction(armed: Boolean, onArm: () -> Unit, onDelete: () -> Unit) {
    val c = LocalAppColors.current
    TextAction(if (armed) "Точно удалить?" else "Удалить", if (armed) onDelete else onArm, c.red)
}

fun hhmm(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
