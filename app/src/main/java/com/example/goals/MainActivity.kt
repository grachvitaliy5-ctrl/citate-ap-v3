package com.example.goals

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private var requestedTab by mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedTab = intent.getIntExtra("tab", -1)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        Notifier.ensureChannels(this)
        Scheduler.scheduleAll(this)

        setContent {
            AppTheme { App(resumeTick, requestedTab) { requestedTab = -1 } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getIntExtra("tab", -1)
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

private val SceneBg = Color(0xFF0B0A1F)

@Composable
fun App(resumeTick: Int, requestedTab: Int, onTabConsumed: () -> Unit) {
    val c = LocalAppColors.current
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(requestedTab) {
        if (requestedTab in 0..4) {
            tab = requestedTab
            onTabConsumed()
        }
    }
    LaunchedEffect(tab) {
        val act = ctx as? ComponentActivity ?: return@LaunchedEffect
        if (tab == 0) {
            act.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        } else {
            act.enableEdgeToEdge()
        }
    }

    Column(Modifier.fillMaxSize().background(if (tab == 0) SceneBg else c.bg).statusBarsPadding().imePadding()) {
        Box(Modifier.weight(1f)) {
            PathHost(visible = tab == 0)
            if (tab != 0) {
                Box(Modifier.fillMaxSize().background(c.bg)) {
                    Crossfade(targetState = tab, animationSpec = tween(180), label = "tabs") { t ->
                        when (t) {
                            1 -> ItemsScreen("goals")
                            2 -> ItemsScreen("quotes")
                            3 -> NotesScreen(resumeTick)
                            else -> SettingsScreen(resumeTick)
                        }
                    }
                }
            }
        }
        TabBar(listOf("Путь", "Цели", "Цитаты", "Заметки", "Ещё"), tab) { tab = it }
    }
}

fun pickTime(ctx: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
}

// ---------- Цели и цитаты ----------

@Composable
fun ItemsScreen(kind: String) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    val list = remember(kind) { mutableStateListOf<Item>().also { it.addAll(store.load(kind)) } }
    var editing by remember { mutableStateOf<Item?>(null) }
    var adding by remember { mutableStateOf(false) }
    var modeTick by remember { mutableIntStateOf(0) }
    val isQuotes = kind == "quotes"

    fun persist() = store.save(kind, list.toList())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.xl)) {
        item {
            ScreenHeader(
                title = if (isQuotes) "Цитаты" else "Цели",
                subtitle = "Напоминаний можно добавить сколько угодно",
                actionLabel = "Добавить",
                onAction = { adding = true }
            )
            Line()
        }
        item { RemindersSection(kind) }
        item {
            val modes = listOf("all" to "Все", "seq" to "По очереди", "random" to "Случайно")
            Text(
                "Что показывать", style = Type.caption, color = c.muted,
                modifier = Modifier.padding(start = Space.m, end = Space.m, top = Space.m, bottom = Space.s)
            )
            val cur = if (modeTick >= 0) store.mode(kind) else ""
            Segmented(modes, cur, { store.setMode(kind, it); modeTick++ }, Modifier.padding(horizontal = Space.m))
            Spacer(Modifier.height(Space.m))
            Line()
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    if (isQuotes) "Цитат пока нет." else "Целей пока нет.",
                    if (isQuotes) "Добавить цитату" else "Добавить цель"
                ) { adding = true }
            }
        }
        items(list, key = { it.id }) { item ->
            Row(
                Modifier.fillMaxWidth().clickable { editing = item }
                    .padding(horizontal = Space.m, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    val tc = if (item.enabled) c.text else c.muted
                    if (isQuotes) {
                        Text("«${item.text}»", style = Type.quote, color = tc)
                        if (item.author.isNotBlank()) {
                            Spacer(Modifier.height(Space.s))
                            Text(item.author.uppercase(), style = Type.label, color = c.muted)
                        }
                    } else {
                        Text(item.text, style = Type.body, color = tc)
                    }
                }
                Spacer(Modifier.width(Space.m))
                Switch(
                    checked = item.enabled,
                    onCheckedChange = { on ->
                        val i = list.indexOfFirst { it.id == item.id }
                        if (i >= 0) {
                            list[i] = item.copy(enabled = on)
                            persist()
                        }
                    },
                    colors = appSwitchColors()
                )
            }
            Line(Modifier.padding(horizontal = Space.m))
        }
    }

    if (adding || editing != null) {
        val cur = editing
        ItemDialog(
            isQuote = isQuotes,
            initial = cur,
            onDismiss = { adding = false; editing = null },
            onSave = { text, author ->
                if (cur != null) {
                    val i = list.indexOfFirst { it.id == cur.id }
                    if (i >= 0) list[i] = cur.copy(text = text, author = author)
                } else {
                    list.add(Item(System.currentTimeMillis(), text, author, true))
                }
                persist()
                adding = false; editing = null
            },
            onDelete = {
                if (cur != null) {
                    list.removeAll { it.id == cur.id }
                    persist()
                }
                adding = false; editing = null
            }
        )
    }
}

@Composable
fun ItemDialog(
    isQuote: Boolean,
    initial: Item?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var author by remember { mutableStateOf(initial?.author ?: "") }
    var armed by remember { mutableStateOf(false) }
    val title = when {
        initial == null && isQuote -> "Новая цитата"
        initial == null -> "Новая цель"
        isQuote -> "Цитата"
        else -> "Цель"
    }
    AppDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = "Сохранить",
        onConfirm = { if (text.isNotBlank()) onSave(text.trim(), author.trim()) }
    ) {
        AppField(text, { text = it }, if (isQuote) "Текст цитаты" else "Цель")
        if (isQuote) AppField(author, { author = it }, "Автор", singleLine = true)
        if (initial != null) DeleteAction(armed, { armed = true }, onDelete)
    }
}

// ---------- Напоминания: без ограничения числа ----------

@Composable
fun RemindersSection(kind: String) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    val list = remember(kind) { mutableStateListOf<Reminder>().also { it.addAll(store.reminders(kind)) } }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun persist() {
        store.saveReminders(kind, list.toList())
        Scheduler.scheduleAll(ctx)
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "НАПОМИНАНИЯ", style = Type.label, color = c.muted,
            modifier = Modifier.padding(start = Space.m, end = Space.m, top = Space.m, bottom = Space.xs)
        )
        list.forEach { r ->
            Row(
                Modifier.fillMaxWidth().clickable { editing = r }
                    .padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    r.describe(), style = Type.body,
                    color = if (r.enabled) c.text else c.muted, modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = r.enabled,
                    onCheckedChange = { on ->
                        val i = list.indexOfFirst { it.id == r.id }
                        if (i >= 0) {
                            list[i] = r.copy(enabled = on)
                            persist()
                        }
                    },
                    colors = appSwitchColors()
                )
            }
        }
        Row(Modifier.padding(horizontal = Space.s)) {
            TextAction("Добавить напоминание", { adding = true })
        }
    }

    if (adding || editing != null) {
        val cur = editing
        ReminderDialog(
            initial = cur,
            onDismiss = { adding = false; editing = null },
            onSave = { r ->
                if (cur != null) {
                    val i = list.indexOfFirst { it.id == cur.id }
                    if (i >= 0) list[i] = r.copy(id = cur.id)
                } else {
                    list.add(r.copy(id = System.currentTimeMillis()))
                }
                persist()
                adding = false; editing = null
            },
            onDelete = {
                if (cur != null) {
                    list.removeAll { it.id == cur.id }
                    persist()
                }
                adding = false; editing = null
            }
        )
    }
}

@Composable
fun ReminderDialog(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    var interval by remember { mutableStateOf(initial?.interval ?: false) }
    var time by remember { mutableIntStateOf(initial?.time ?: (9 * 60)) }
    var every by remember { mutableStateOf((initial?.everyMin ?: 60).toString()) }
    var from by remember { mutableIntStateOf(initial?.from ?: (9 * 60)) }
    var to by remember { mutableIntStateOf(initial?.to ?: (21 * 60)) }
    var armed by remember { mutableStateOf(false) }

    AppDialog(
        title = if (initial == null) "Новое напоминание" else "Напоминание",
        onDismiss = onDismiss,
        confirmLabel = "Сохранить",
        onConfirm = {
            val e = (every.toIntOrNull() ?: 60).coerceIn(1, 1440)
            val end = maxOf(to, from + 1).coerceAtMost(1439)
            onSave(Reminder(initial?.id ?: 0L, interval, time, e, from, end, initial?.enabled ?: true))
        }
    ) {
        Segmented(listOf(false to "В заданное время", true to "С интервалом"), interval, { interval = it })
        if (!interval) {
            ValueRowCompact("Время", hhmm(time)) { pickTime(ctx, time) { time = it } }
        } else {
            AppField(every, { v -> every = v.filter { it.isDigit() }.take(4) }, "Каждые, минут", singleLine = true)
            ValueRowCompact("С", hhmm(from)) { pickTime(ctx, from) { from = it } }
            ValueRowCompact("До", hhmm(to)) { pickTime(ctx, to) { to = it } }
            Text("Напоминание повторяется в этом окне каждый день.", style = Type.caption, color = c.muted)
        }
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
