package com.example.goals

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        Notifier.ensureChannels(this)
        Scheduler.scheduleAll(this)

        setContent {
            AppTheme { App(resumeTick) }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun App(resumeTick: Int) {
    val c = LocalAppColors.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = tab, animationSpec = tween(180), label = "tabs") { t ->
                when (t) {
                    0 -> ItemsScreen("goals")
                    1 -> ItemsScreen("quotes")
                    2 -> RoutinesScreen(resumeTick)
                    else -> SettingsScreen(resumeTick)
                }
            }
        }
        TabBar(listOf("Цели", "Цитаты", "Рутины", "Настройки"), tab) { tab = it }
    }
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
    val isQuotes = kind == "quotes"

    fun persist() = store.save(kind, list.toList())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.xl)) {
        item {
            ScreenHeader(
                title = if (isQuotes) "Цитаты" else "Цели",
                subtitle = "Напоминания в ${hhmm(store.time(kind, 0))} и ${hhmm(store.time(kind, 1))}",
                actionLabel = "Добавить",
                onAction = { adding = true }
            )
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

// ---------- Настройки ----------

fun pickTime(ctx: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
}

@Composable
fun SettingsScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    var tick by remember { mutableIntStateOf(0) }
    // Чтение tick и resumeTick заставляет экран перерисоваться после изменений.
    val refresh = tick + resumeTick

    val modes = listOf("all" to "Все", "seq" to "По очереди", "random" to "Случайно")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Space.xl)
    ) {
        ScreenHeader("Настройки")
        Line()

        for ((kind, title) in listOf("goals" to "Цели", "quotes" to "Цитаты")) {
            SectionLabel(title)
            for (slot in 0..1) {
                ValueRow(
                    if (slot == 0) "Первое напоминание" else "Второе напоминание",
                    hhmm(store.time(kind, slot))
                ) {
                    pickTime(ctx, store.time(kind, slot)) {
                        store.setTime(kind, slot, it)
                        Scheduler.scheduleAll(ctx)
                        tick++
                    }
                }
            }
            Text(
                "Что показывать", style = Type.caption, color = c.muted,
                modifier = Modifier.padding(start = Space.m, end = Space.m, top = Space.s, bottom = Space.s)
            )
            Segmented(
                modes, store.mode(kind), { store.setMode(kind, it); tick++ },
                Modifier.padding(horizontal = Space.m)
            )
            if (kind == "goals") {
                Spacer(Modifier.height(Space.s))
                ToggleRow(
                    "Свой звук", "Короткий светлый звук «рассвет»",
                    store.goalSound
                ) { store.goalSound = it; tick++ }
            }
            Spacer(Modifier.height(Space.s))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.m),
                horizontalArrangement = Arrangement.spacedBy(Space.s)
            ) {
                if (kind == "goals") {
                    GhostButton("Прослушать", { Notifier.previewGoalSound(ctx) }, Modifier.weight(1f))
                }
                GhostButton("Проверить", { Notifier.show(ctx, kind) }, Modifier.weight(1f))
            }
        }

        SectionLabel("Рутины")
        Text(
            "Начало нового дня", style = Type.caption, color = c.muted,
            modifier = Modifier.padding(start = Space.m, end = Space.m, bottom = Space.s)
        )
        Segmented(
            listOf(0 to "00:00", 4 to "04:00"), store.dayStartHour,
            { store.dayStartHour = it; tick++ },
            Modifier.padding(horizontal = Space.m)
        )
        Spacer(Modifier.height(Space.s))
        ToggleRow("Вечернее напоминание", "В 21:00, если день не отмечен", store.eveningReminder) {
            store.eveningReminder = it; tick++
        }
        ToggleRow("Итог недели", "В воскресенье в 21:00", store.weeklySummary) {
            store.weeklySummary = it; tick++
        }

        SectionLabel("Система")
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pm = ctx.getSystemService(PowerManager::class.java)
        val notifOk = refresh >= 0 && nm.areNotificationsEnabled()
        val exactOk = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        val batteryOk = pm.isIgnoringBatteryOptimizations(ctx.packageName)

        PermissionRow("Уведомления", notifOk) {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
        if (Build.VERSION.SDK_INT >= 31) {
            PermissionRow("Точные будильники", exactOk) {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                )
            }
        }
        PermissionRow("Работа без ограничений батареи", batteryOk) {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        Text(
            "Громкость звука задаётся ползунком уведомлений на телефоне. " +
                "На Xiaomi, Samsung, Huawei также включите автозапуск в настройках приложения.",
            style = Type.caption, color = c.muted,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)
        )
    }
}

@Composable
fun PermissionRow(title: String, ok: Boolean, onFix: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !ok, onClick = onFix)
            .padding(horizontal = Space.m, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = Type.body, color = c.text, modifier = Modifier.weight(1f))
        Text(
            if (ok) "Включено" else "Включить",
            style = Type.body.copy(fontWeight = FontWeight.Medium),
            color = if (ok) c.muted else c.accent
        )
    }
}
