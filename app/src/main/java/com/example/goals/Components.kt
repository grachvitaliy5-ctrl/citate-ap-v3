package com.example.goals

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
        Modifier.fillMaxWidth().padding(start = Space.m, end = Space.s, top = Space.l, bottom = Space.m),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.title, color = c.text)
            if (subtitle != null) {
                Spacer(Modifier.height(Space.xs))
                Text(subtitle, style = Type.caption, color = c.muted)
            }
        }
        if (actionLabel != null && onAction != null) TextAction(actionLabel, onAction)
    }
}

@Composable
fun SectionLabel(text: String) {
    val c = LocalAppColors.current
    Text(
        text.uppercase(), style = Type.label, color = c.muted,
        modifier = Modifier.padding(start = Space.m, end = Space.m, top = Space.xl, bottom = Space.s)
    )
}

@Composable
fun Line(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = LocalAppColors.current.line)
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, color: Color = LocalAppColors.current.accent) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(AppShape).clickable(onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.s),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Type.body.copy(fontWeight = FontWeight.Medium), color = color)
    }
}

@Composable
fun FlatButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = LocalAppColors.current
    val bg by animateColorAsState(if (enabled) c.accent else c.fog, tween(180), label = "btn")
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(AppShape).background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.m, vertical = Space.s),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, style = Type.body.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) c.onAccent else c.muted, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Box(
        modifier.heightIn(min = 48.dp).clip(AppShape).border(1.dp, c.line, AppShape)
            .clickable(onClick = onClick).padding(horizontal = Space.m, vertical = Space.s),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Type.body, color = c.text, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(text: String, actionLabel: String, onAction: () -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.l)) {
        Text(text, style = Type.body, color = c.muted)
        Spacer(Modifier.height(Space.m))
        GhostButton(actionLabel, onAction)
    }
}

@Composable
fun appSwitchColors(): SwitchColors {
    val c = LocalAppColors.current
    return SwitchDefaults.colors(
        checkedThumbColor = c.onAccent,
        checkedTrackColor = c.accent,
        checkedBorderColor = c.accent,
        uncheckedThumbColor = c.muted,
        uncheckedTrackColor = c.fog,
        uncheckedBorderColor = c.line
    )
}

@Composable
fun appFieldColors(): TextFieldColors {
    val c = LocalAppColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.line,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.muted,
        cursorColor = c.accent
    )
}

@Composable
fun AppField(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
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
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.body, color = c.text)
            if (subtitle != null) Text(subtitle, style = Type.caption, color = c.muted)
        }
        Spacer(Modifier.width(Space.s))
        Switch(checked = checked, onCheckedChange = onChange, colors = appSwitchColors())
    }
}

@Composable
fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = Type.body, color = c.text, modifier = Modifier.weight(1f))
        Text(value, style = Type.body.copy(fontWeight = FontWeight.Medium), color = c.accent)
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
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(AppShape).border(1.dp, c.line, AppShape)
    ) {
        options.forEachIndexed { i, (value, label) ->
            val sel = value == selected
            val bg by animateColorAsState(if (sel) c.text else Color.Transparent, tween(150), label = "seg")
            Box(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).background(bg)
                    .clickable { onSelect(value) }.padding(horizontal = Space.xs, vertical = Space.s),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = Type.caption, color = if (sel) c.bg else c.text, textAlign = TextAlign.Center)
            }
            if (i < options.size - 1) Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
        }
    }
}

// ---------- Навигация ----------

@Composable
fun TabBar(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    Column(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding()) {
        Line()
        Row(Modifier.fillMaxWidth()) {
            titles.forEachIndexed { i, t ->
                val sel = i == selected
                val col by animateColorAsState(if (sel) c.text else c.muted, tween(180), label = "tab")
                val bar by animateColorAsState(if (sel) c.accent else Color.Transparent, tween(180), label = "bar")
                Box(
                    Modifier.weight(1f).heightIn(min = 56.dp).clickable { onSelect(i) }
                        .padding(vertical = Space.s),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            t, color = col, textAlign = TextAlign.Center,
                            style = Type.caption.copy(fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(16.dp).height(2.dp).background(bar))
                    }
                }
            }
        }
    }
}

// ---------- Рутины: статус, шкала, карта ----------

@Composable
fun statusColor(s: Status): Color {
    val c = LocalAppColors.current
    return when (s) {
        Status.GREEN -> c.green
        Status.YELLOW -> c.yellow
        Status.RED -> c.red
    }
}

/** Статус: цвет + форма значка + текст (не только цвет). */
@Composable
fun StatusBadge(status: Status) {
    val c = LocalAppColors.current
    val col = statusColor(status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) {
            val w = 2.dp.toPx()
            when (status) {
                Status.GREEN -> drawCircle(col)
                Status.YELLOW -> drawCircle(col, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
                Status.RED -> {
                    drawLine(col, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = w)
                    drawLine(col, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = w)
                }
            }
        }
        Spacer(Modifier.width(Space.s))
        Text(statusText(status), style = Type.caption, color = c.text)
    }
}

@Composable
fun ThinProgress(fraction: Float, color: Color) {
    val c = LocalAppColors.current
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(250), label = "progress")
    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.fog)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(color))
    }
}

/**
 * Карта прогресса: 7 клеток в ряд (ряд = неделя).
 * Скрытые клетки — «туман». Открытые — цвет статуса и фактура:
 * зелёный — сплошная, жёлтый — пунктирная рамка, красный — крест.
 * Конец каждой недели — контрольная точка (черта под клеткой). Последняя клетка — финал (рамка акцентом).
 */
@Composable
fun ProgressMap(total: Int, opened: Int, status: Status, revealAll: Boolean) {
    val c = LocalAppColors.current
    val fill = statusColor(status)
    val cols = 7
    val rows = (total + cols - 1) / cols

    val reveal = remember { Animatable(1f) }
    var prev by remember { mutableIntStateOf(opened) }
    LaunchedEffect(opened) {
        if (opened > prev) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(200))
        }
        prev = opened
    }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(status) {
        if (status == Status.RED) {
            flash.snapTo(0.3f)
            flash.animateTo(1f, tween(250))
        }
    }

    val desc = "Карта: открыто $opened из $total. ${statusText(status)}"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val cell = (maxWidth - gap * (cols - 1)) / cols
        val mapHeight = cell * rows + gap * rows
        Canvas(Modifier.fillMaxWidth().height(mapHeight).semantics { contentDescription = desc }) {
            val g = gap.toPx()
            val s = cell.toPx()
            val r = CornerRadius(3.dp.toPx())
            val thin = 1.dp.toPx()
            val mid = 1.5.dp.toPx()
            for (i in 0 until total) {
                val col = i % cols
                val row = i / cols
                val tl = Offset(col * (s + g), row * (s + g))
                val sz = Size(s, s)
                val isOpen = revealAll || i < opened
                val checkpoint = (i + 1) % 7 == 0
                val last = i == total - 1
                if (isOpen) {
                    val a = (if (!revealAll && i == opened - 1) reveal.value else 1f) * flash.value
                    drawRoundRect(fill.copy(alpha = a), tl, sz, r)
                    when (status) {
                        Status.YELLOW -> {
                            val inset = s * 0.24f
                            drawRoundRect(
                                c.bg.copy(alpha = 0.9f * a),
                                Offset(tl.x + inset, tl.y + inset),
                                Size(s - 2 * inset, s - 2 * inset),
                                r,
                                style = Stroke(
                                    width = mid,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
                                )
                            )
                        }
                        Status.RED -> {
                            val inset = s * 0.3f
                            val lc = c.bg.copy(alpha = a)
                            drawLine(lc, Offset(tl.x + inset, tl.y + inset), Offset(tl.x + s - inset, tl.y + s - inset), strokeWidth = mid)
                            drawLine(lc, Offset(tl.x + s - inset, tl.y + inset), Offset(tl.x + inset, tl.y + s - inset), strokeWidth = mid)
                        }
                        Status.GREEN -> Unit
                    }
                } else {
                    drawRoundRect(c.fog, tl, sz, r)
                }
                if (checkpoint) {
                    val y = tl.y + s + g / 2
                    drawLine(c.muted, Offset(tl.x + s * 0.3f, y), Offset(tl.x + s * 0.7f, y), strokeWidth = thin)
                }
                if (last) drawRoundRect(c.accent, tl, sz, r, style = Stroke(width = mid))
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = AppShape,
        title = { Text(title, style = Type.heading, color = c.text) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(Space.m)) { content() } },
        confirmButton = { TextAction(confirmLabel, onConfirm) },
        dismissButton = { TextAction("Отмена", onDismiss, c.muted) }
    )
}

/** Удаление в два нажатия, чтобы не удалить случайно. */
@Composable
fun DeleteAction(armed: Boolean, onArm: () -> Unit, onDelete: () -> Unit) {
    val c = LocalAppColors.current
    TextAction(if (armed) "Точно удалить?" else "Удалить", if (armed) onDelete else onArm, c.red)
}

fun hhmm(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
