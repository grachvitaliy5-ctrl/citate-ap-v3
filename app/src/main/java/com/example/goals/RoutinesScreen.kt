package com.example.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun RoutinesScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var routines by remember { mutableStateOf(store.refreshRoutines()) }
    var today by remember { mutableStateOf(store.today()) }
    LaunchedEffect(resumeTick) {
        routines = store.refreshRoutines()
        today = store.today()
    }
    var editing by remember { mutableStateOf<Routine?>(null) }
    var adding by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.xl)) {
        item {
            ScreenHeader(
                title = "Рутины",
                subtitle = "Каждая отметка открывает клетку карты",
                actionLabel = "Добавить",
                onAction = { adding = true }
            )
            Line()
        }
        if (routines.isEmpty()) {
            item { EmptyState("Рутин пока нет.", "Добавить рутину") { adding = true } }
        }
        items(routines, key = { it.id }) { r ->
            RoutineBlock(
                r = r,
                today = today,
                onMark = { routines = store.markToday(r.id) },
                onEdit = { editing = r },
                onAckReset = { routines = store.updateRoutine(r.id) { it.copy(pendingReset = null) } },
                onNewCycle = {
                    val t = store.today()
                    routines = store.updateRoutine(r.id) { RoutineLogic.newCycle(it, t) }
                }
            )
            Line(Modifier.padding(horizontal = Space.m))
        }
    }

    if (adding || editing != null) {
        val cur = editing
        RoutineDialog(
            initial = cur,
            onDismiss = { adding = false; editing = null },
            onSave = { title, cycle, time ->
                routines = if (cur == null) {
                    store.addRoutine(title, cycle, time)
                } else {
                    store.updateRoutine(cur.id) {
                        it.copy(title = title, time = time, cycleLength = maxOf(cycle, it.progress))
                    }
                }
                Scheduler.scheduleAll(ctx)
                adding = false; editing = null
            },
            onDelete = {
                if (cur != null) {
                    Scheduler.cancelRoutine(ctx, cur.id)
                    routines = store.deleteRoutine(cur.id)
                }
                adding = false; editing = null
            }
        )
    }
}

@Composable
fun RoutineBlock(
    r: Routine,
    today: Long,
    onMark: () -> Unit,
    onEdit: () -> Unit,
    onAckReset: () -> Unit,
    onNewCycle: () -> Unit
) {
    val c = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val state = RoutineLogic.state(r, today)
    val pending = r.pendingReset
    val status = if (pending != null) Status.RED else state.status
    val opened = if (pending != null) minOf(pending.days, r.cycleLength) else r.progress
    val doneToday = r.doneOn(today)

    Column(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.title, style = Type.heading, color = c.text, modifier = Modifier.weight(1f))
            TextAction("Изменить", onEdit, c.muted)
        }
        StatusBadge(status)
        Spacer(Modifier.height(Space.m))

        Row(verticalAlignment = Alignment.Bottom) {
            Text("День $opened из ${r.cycleLength}", style = Type.body, color = c.text, modifier = Modifier.weight(1f))
            Text("Попытка ${r.attemptNumber}", style = Type.caption, color = c.muted)
        }
        Spacer(Modifier.height(Space.s))
        ThinProgress(opened.toFloat() / r.cycleLength, statusColor(status))
        Spacer(Modifier.height(Space.m))

        ProgressMap(total = r.cycleLength, opened = opened, status = status, revealAll = r.finished)
        Spacer(Modifier.height(Space.s))

        val streak = if (pending != null) 0 else state.streak
        Text(
            "Серия: ${days(streak)} · Лучший результат: ${days(r.bestStreak)}",
            style = Type.caption, color = c.muted
        )
        if (pending == null && !r.finished && status == Status.YELLOW) {
            Spacer(Modifier.height(Space.xs))
            Text(
                "Ещё ${days(state.recoverLeft)} подряд — и карта снова зелёная",
                style = Type.caption, color = c.text
            )
        }
        Spacer(Modifier.height(Space.m))

        when {
            pending != null -> {
                Text(
                    "Цикл сброшен. Попытка ${pending.attempt}: ${days(pending.days)} — сохранена в истории. " +
                        "Новый цикл начинается сегодня.",
                    style = Type.body, color = c.text
                )
                Spacer(Modifier.height(Space.m))
                FlatButton("Начать новый цикл", onAckReset)
            }
            r.finished -> {
                Text("Цикл пройден: ${days(r.cycleLength)}", style = Type.heading, color = c.text)
                Spacer(Modifier.height(Space.xs))
                Text("Пропуски: ${state.misses} · Сбросы: ${r.resets}", style = Type.caption, color = c.muted)
                Spacer(Modifier.height(Space.m))
                FlatButton("Начать новый цикл", onNewCycle)
            }
            else -> {
                FlatButton(
                    if (doneToday) "Сегодня отмечено" else "Отметить сегодня",
                    onClick = {
                        val next = r.progress + 1
                        if (next % 7 == 0 || next >= r.cycleLength) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onMark()
                    },
                    enabled = !doneToday
                )
            }
        }

        if (r.history.isNotEmpty()) {
            Spacer(Modifier.height(Space.m))
            Text("ИСТОРИЯ", style = Type.label, color = c.muted)
            Spacer(Modifier.height(Space.xs))
            r.history.takeLast(3).reversed().forEach { a ->
                Text(
                    "Попытка ${a.number}: ${days(a.days)}" + if (a.completed) " — цикл пройден" else "",
                    style = Type.caption, color = c.muted
                )
            }
        }
    }
}

@Composable
fun RoutineDialog(
    initial: Routine?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var cycle by remember { mutableIntStateOf(initial?.cycleLength ?: 30) }
    var time by remember { mutableIntStateOf(initial?.time ?: (8 * 60)) }
    var armed by remember { mutableStateOf(false) }

    AppDialog(
        title = if (initial == null) "Новая рутина" else "Рутина",
        onDismiss = onDismiss,
        confirmLabel = "Сохранить",
        onConfirm = { if (title.isNotBlank()) onSave(title.trim(), cycle, time) }
    ) {
        AppField(title, { title = it }, "Название", singleLine = true)
        Column {
            Text("Длина цикла", style = Type.caption, color = c.muted)
            Spacer(Modifier.height(Space.s))
            Segmented(listOf(21 to "21", 30 to "30", 66 to "66"), cycle, { cycle = it })
        }
        ValueRowCompact("Напоминание", hhmm(time)) { pickTime(ctx, time) { time = it } }
        if (initial != null) DeleteAction(armed, { armed = true }, onDelete)
    }
}

@Composable
fun ValueRowCompact(title: String, value: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = Type.body, color = c.text, modifier = Modifier.weight(1f))
        TextAction(value, onClick)
    }
}
