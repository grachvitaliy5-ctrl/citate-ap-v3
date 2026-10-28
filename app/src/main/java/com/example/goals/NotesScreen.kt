package com.example.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private val MONTHS = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val MONTHS_GEN = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)
private val DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/** Заметки привязаны к датам календаря; у заметки может быть напоминание в выбранное время. */
@Composable
fun NotesScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    val notes = remember { mutableStateListOf<Note>().also { it.addAll(store.notes()) } }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var adding by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    fun persist() {
        store.saveNotes(notes.toList())
        Scheduler.scheduleAll(ctx)
    }

    val daysWithNotes = notes.map { it.day }.toSet()
    val dayNotes = notes.filter { it.day == selected.toEpochDay() }.sortedBy { it.time }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.xl)) {
        item {
            ScreenHeader(
                title = "Заметки",
                subtitle = "Привязаны к датам календаря",
                actionLabel = "Добавить",
                onAction = { adding = true }
            )
            Line()
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextAction("‹", { month = month.minusMonths(1) }, c.text)
                Box(
                    Modifier.weight(1f).clickable {
                        month = YearMonth.now()
                        selected = LocalDate.now()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${MONTHS[month.monthValue - 1]} ${month.year}", style = Type.heading, color = c.text)
                }
                TextAction("›", { month = month.plusMonths(1) }, c.text)
            }
            Column(Modifier.padding(horizontal = Space.s)) {
                Row(Modifier.fillMaxWidth()) {
                    DOW.forEach {
                        Text(
                            it, style = Type.label, color = c.muted, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).padding(vertical = Space.xs)
                        )
                    }
                }
                val offset = month.atDay(1).dayOfWeek.value - 1
                val len = month.lengthOfMonth()
                val rows = (offset + len + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val dayNum = r * 7 + col - offset + 1
                            if (dayNum < 1 || dayNum > len) {
                                Spacer(Modifier.weight(1f).height(48.dp))
                            } else {
                                val date = month.atDay(dayNum)
                                DayCell(
                                    num = dayNum,
                                    selected = date == selected,
                                    isToday = date == today,
                                    hasNotes = daysWithNotes.contains(date.toEpochDay()),
                                    modifier = Modifier.weight(1f)
                                ) { selected = date }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.s))
            Line()
        }
        item {
            SectionLabel("${selected.dayOfMonth} ${MONTHS_GEN[selected.monthValue - 1]} ${selected.year}")
        }
        if (dayNotes.isEmpty()) {
            item { EmptyState("На этот день заметок нет.", "Добавить заметку") { adding = true } }
        }
        items(dayNotes, key = { it.id }) { n ->
            Column(
                Modifier.fillMaxWidth().clickable { editing = n }
                    .padding(horizontal = Space.m, vertical = Space.m)
            ) {
                Text(n.text, style = Type.body, color = c.text)
                if (n.time >= 0) {
                    Spacer(Modifier.height(Space.xs))
                    Text("Напоминание в ${hhmm(n.time)}", style = Type.caption, color = c.muted)
                }
            }
            Line(Modifier.padding(horizontal = Space.m))
        }
    }

    if (adding || editing != null) {
        val cur = editing
        NoteDialog(
            initial = cur,
            onDismiss = { adding = false; editing = null },
            onSave = { text, time ->
                if (cur != null) {
                    val i = notes.indexOfFirst { it.id == cur.id }
                    if (i >= 0) notes[i] = cur.copy(text = text, time = time)
                } else {
                    notes.add(Note(System.currentTimeMillis(), selected.toEpochDay(), text, time))
                }
                persist()
                adding = false; editing = null
            },
            onDelete = {
                if (cur != null) {
                    notes.removeAll { it.id == cur.id }
                    persist()
                }
                adding = false; editing = null
            }
        )
    }
}

@Composable
private fun DayCell(
    num: Int,
    selected: Boolean,
    isToday: Boolean,
    hasNotes: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    val bg = if (selected) c.text else Color.Transparent
    val fg = if (selected) c.bg else c.text
    var m = modifier.height(48.dp).clip(AppShape).background(bg)
    if (isToday && !selected) m = m.border(1.dp, c.accent, AppShape)
    Box(m.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(num.toString(), style = Type.body, color = fg)
            Box(
                Modifier.size(5.dp).background(
                    if (hasNotes) (if (selected) c.bg else c.accent) else Color.Transparent,
                    CircleShape
                )
            )
        }
    }
}

@Composable
fun NoteDialog(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var remind by remember { mutableStateOf((initial?.time ?: -1) >= 0) }
    var time by remember { mutableIntStateOf(if ((initial?.time ?: -1) >= 0) initial!!.time else 9 * 60) }
    var armed by remember { mutableStateOf(false) }

    AppDialog(
        title = if (initial == null) "Новая заметка" else "Заметка",
        onDismiss = onDismiss,
        confirmLabel = "Сохранить",
        onConfirm = { if (text.isNotBlank()) onSave(text.trim(), if (remind) time else -1) }
    ) {
        AppField(text, { text = it }, "Текст заметки")
        ToggleRow("Напомнить", "В выбранное время этой даты", remind) { remind = it }
        if (remind) ValueRowCompact("Время", hhmm(time)) { pickTime(ctx, time) { time = it } }
        if (initial != null) DeleteAction(armed, { armed = true }, onDelete)
    }
}
