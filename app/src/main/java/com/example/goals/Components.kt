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
