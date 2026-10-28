package com.example.goals

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FUNCTIONS = listOf(
    "goals" to "Цели",
    "quotes" to "Цитаты",
    "routines" to "Рутины",
    "notes" to "Заметки"
)

private fun soundLabel(store: Store, fn: String): String {
    val v = store.sound(fn)
    return when {
        v == "welcome" -> "Мелодия самурая"
        v == "silent" -> "Без мелодии"
        v.startsWith("m:") -> {
            val id = v.substring(2).toLongOrNull()
            store.melodies().firstOrNull { it.id == id }?.title() ?: "Мелодия самурая"
        }
        else -> "Мелодия самурая"
    }
}

@Composable
fun SettingsScreen(resumeTick: Int) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    var tick by remember { mutableIntStateOf(0) }
    // Чтение tick и resumeTick заставляет экран перерисоваться после изменений.
    val refresh = tick + resumeTick
    var melodyFor by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Space.xl)
    ) {
        ScreenHeader("Ещё")
        Line()

        SectionLabel("Мелодии уведомлений")
        Text(
            "У каждой функции своя мелодия. По умолчанию звучит мелодия самурая. " +
                "Можно выбрать любую мелодию острова, которую вы собрали на вкладке «Путь».",
            style = Type.caption, color = c.muted,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)
        )
        for ((fn, title) in FUNCTIONS) {
            ValueRow(title, if (refresh >= 0) soundLabel(store, fn) else "") { melodyFor = fn }
        }
        Spacer(Modifier.height(Space.s))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.m),
            horizontalArrangement = Arrangement.spacedBy(Space.s)
        ) {
            GhostButton("Проверить цели", {
                Thread {
                    if (Notifier.showItems(ctx, "goals")) Notifier.playCustom(ctx, "goals")
                }.start()
            }, Modifier.weight(1f))
            GhostButton("Проверить цитаты", {
                Thread {
                    if (Notifier.showItems(ctx, "quotes")) Notifier.playCustom(ctx, "quotes")
                }.start()
            }, Modifier.weight(1f))
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
        ToggleRow("Вечернее напоминание", "В 21:00, если остались незажжённые фонари", store.eveningReminder) {
            store.eveningReminder = it
            Scheduler.scheduleAll(ctx)
            tick++
        }
        Text(
            "Название, фонарь и время напоминания каждой рутины настраиваются кнопкой «Редактор» на вкладке «Путь».",
            style = Type.caption, color = c.muted,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)
        )

        SectionLabel("Сцена")
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.m)) {
            GhostButton("Открыть демо сцены", {
                ctx.startActivity(Intent(ctx, DemoActivity::class.java))
            }, Modifier.fillMaxWidth())
        }
        Text(
            "В демо можно перемотать цикл и сменить день. Настоящий прогресс при этом не меняется.",
            style = Type.caption, color = c.muted,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.s)
        )

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

    val fn = melodyFor
    if (fn != null) {
        MelodyDialog(
            fn = fn,
            title = FUNCTIONS.first { it.first == fn }.second,
            onDismiss = { melodyFor = null },
            onChanged = { tick++ }
        )
    }
}

@Composable
fun MelodyDialog(fn: String, title: String, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val store = remember { Store(ctx) }
    val melodies = remember { store.melodies() }
    var current by remember { mutableStateOf(store.sound(fn)) }

    fun choose(v: String) {
        store.setSound(fn, v)
        current = v
        onChanged()
    }

    AppDialog(
        title = "Мелодия: $title",
        onDismiss = onDismiss,
        confirmLabel = "Готово",
        onConfirm = onDismiss
    ) {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            MelodyOption(
                "Мелодия самурая", "По умолчанию", current == "welcome",
                { choose("welcome") }, { Melody.previewWelcome(ctx) }
            )
            MelodyOption("Без мелодии", "Только вибрация", current == "silent", { choose("silent") }, null)
            melodies.forEach { m ->
                val v = "m:${m.id}"
                MelodyOption(
                    m.title(),
                    if (m.bad) "Звучит фальшиво" else "Собрана на острове",
                    current == v,
                    { choose(v) },
                    { Melody.play(ctx, m) }
                )
            }
            if (melodies.isEmpty()) {
                Text(
                    "Мелодии островов появятся здесь, когда вы соберёте первую.",
                    style = Type.caption, color = c.muted,
                    modifier = Modifier.padding(top = Space.s)
                )
            }
        }
    }
}

@Composable
private fun MelodyOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: (() -> Unit)?
) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "●" else "○", style = Type.body, color = if (selected) c.accent else c.muted)
        Column(Modifier.weight(1f).padding(horizontal = Space.s)) {
            Text(title, style = Type.body, color = c.text)
            Text(subtitle, style = Type.caption, color = c.muted)
        }
        if (onPlay != null) TextAction("▶", onPlay)
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
