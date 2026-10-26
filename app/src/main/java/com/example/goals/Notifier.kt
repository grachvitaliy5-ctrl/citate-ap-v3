package com.example.goals

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import java.time.DayOfWeek
import java.time.LocalDate

object Notifier {
    // Звук канала нельзя изменить после создания (Android 8+).
    // При смене звука увеличьте версию ID (goals_sound_v2) — старый канал удалится ниже.
    const val CH_GOALS = "goals_sound_v1"
    const val CH_GOALS_SILENT = "goals_silent_v1"
    const val CH_QUOTES = "quotes_v1"
    const val CH_ROUTINES = "routines_v1"
    private val OLD_CHANNELS = listOf("reminders")

    private const val ID_GOALS = 1
    private const val ID_QUOTES = 2
    private const val ID_EVENING = 3
    private const val ID_WEEKLY = 4

    private fun goalSoundUri(ctx: Context): Uri =
        Uri.parse("android.resource://${ctx.packageName}/${R.raw.goal_sound}")

    private fun soundAttrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        OLD_CHANNELS.forEach { nm.deleteNotificationChannel(it) }

        val goals = NotificationChannel(CH_GOALS, "Цели", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Напоминания о целях со своим звуком"
            setSound(goalSoundUri(ctx), soundAttrs())
            enableVibration(true)
        }
        val goalsSilent = NotificationChannel(CH_GOALS_SILENT, "Цели без звука", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Напоминания о целях: только вибрация"
            setSound(null, null)
            enableVibration(true)
        }
        val quotes = NotificationChannel(CH_QUOTES, "Цитаты", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Цитаты дня"
        }
        val routines = NotificationChannel(CH_ROUTINES, "Рутины", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Ежедневные рутины, вечерние напоминания и итог недели"
        }
        nm.createNotificationChannels(listOf(goals, goalsSilent, quotes, routines))
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 100, Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    // ---------- Цели и цитаты: логика первой версии ----------

    fun show(ctx: Context, kind: String) {
        val store = Store(ctx)
        val items = store.load(kind).filter { it.enabled }
        if (items.isEmpty()) return

        ensureChannels(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)

        fun fmt(i: Item): String =
            if (kind == "quotes")
                "«${i.text}»" + if (i.author.isNotBlank()) " — ${i.author}" else ""
            else i.text

        val body = when (store.mode(kind)) {
            "seq" -> fmt(items[store.nextIndex(kind, items.size)])
            "random" -> fmt(items.random())
            else -> items.joinToString("\n") { "• " + fmt(it) }
        }

        val channel = when {
            kind == "quotes" -> CH_QUOTES
            store.goalSound -> CH_GOALS
            else -> CH_GOALS_SILENT
        }

        val n = Notification.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(if (kind == "goals") "Мои цели" else "Цитата")
            .setContentText(body.lines().first())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()

        nm.notify(if (kind == "goals") ID_GOALS else ID_QUOTES, n)
    }

    // ---------- Рутины ----------

    fun routineNotifId(id: Long): Int = 10_000 + (id % 100_000).toInt()

    fun showRoutine(ctx: Context, id: Long) {
        val store = Store(ctx)
        val r = store.refreshRoutines().firstOrNull { it.id == id } ?: return
        val today = store.today()
        if (r.finished || r.doneOn(today)) return

        ensureChannels(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val nid = routineNotifId(id)

        val done = PendingIntent.getBroadcast(
            ctx, nid,
            Intent(ctx, RoutineActionReceiver::class.java)
                .setAction(RoutineActionReceiver.ACTION_DONE).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze = PendingIntent.getBroadcast(
            ctx, nid + 100_000,
            Intent(ctx, RoutineActionReceiver::class.java)
                .setAction(RoutineActionReceiver.ACTION_SNOOZE).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon = Icon.createWithResource(ctx, R.drawable.ic_notify)
        val text = "День ${r.progress + 1} из ${r.cycleLength}. Отметьте выполнение."

        val n = Notification.Builder(ctx, CH_ROUTINES)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, "Сделано", done).build())
            .addAction(Notification.Action.Builder(icon, "Отложить на час", snooze).build())
            .build()
        nm.notify(nid, n)
    }

    /** Вечером: предупреждение о неотмеченных рутинах; в воскресенье — итог недели. */
    fun showEvening(ctx: Context) {
        val store = Store(ctx)
        val list = store.refreshRoutines()
        if (list.isEmpty()) return
        val today = store.today()
        ensureChannels(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)

        if (store.eveningReminder) {
            val lines = list.filter { !it.finished && !it.doneOn(today) }.map { r ->
                val st = RoutineLogic.state(r, today)
                val what = if (st.status == Status.YELLOW) "карта сбросится" else "карта станет жёлтой"
                "${r.title}: $what"
            }
            if (lines.isNotEmpty()) {
                val body = lines.joinToString("\n")
                val n = Notification.Builder(ctx, CH_ROUTINES)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle("Сегодня не отмечено")
                    .setContentText(lines.first())
                    .setStyle(Notification.BigTextStyle().bigText(body))
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setContentIntent(openApp(ctx))
                    .setAutoCancel(true)
                    .build()
                nm.notify(ID_EVENING, n)
            }
        }

        if (store.weeklySummary && LocalDate.now().dayOfWeek == DayOfWeek.SUNDAY) {
            val lines = list.map { r ->
                val done = (0..6).count { r.doneOn(today - it) }
                val st = RoutineLogic.state(r, today)
                "${r.title}: $done из 7, ${statusText(st.status).lowercase()}"
            }
            val body = lines.joinToString("\n")
            val n = Notification.Builder(ctx, CH_ROUTINES)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("Итог недели")
                .setContentText(lines.first())
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setContentIntent(openApp(ctx))
                .setAutoCancel(true)
                .build()
            nm.notify(ID_WEEKLY, n)
        }
    }

    fun cancel(ctx: Context, notifId: Int) {
        ctx.getSystemService(NotificationManager::class.java).cancel(notifId)
    }

    /** Прослушать звук целей (освобождает плеер после воспроизведения). */
    fun previewGoalSound(ctx: Context) {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(soundAttrs())
            mp.setDataSource(ctx, goalSoundUri(ctx))
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { p, _, _ ->
                p.release()
                true
            }
            mp.setOnPreparedListener { it.start() }
            mp.prepareAsync()
        } catch (e: Exception) {
            mp.release()
        }
    }
}

fun statusText(s: Status): String = when (s) {
    Status.GREEN -> "Без пропусков"
    Status.YELLOW -> "Был пропуск"
    Status.RED -> "Сброс"
}
