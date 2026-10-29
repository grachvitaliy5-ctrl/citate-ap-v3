package com.example.goals

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes

/**
 * Звук канала нельзя изменить после создания (Android 8+).
 * Чтобы сменить звук, увеличьте версию ID канала и добавьте старый ID в OLD_CHANNELS.
 *
 * Мелодия самурая звучит как звук канала. Мелодию острова приложение играет само
 * (Melody.playBlocking) при показе уведомления, а канал при этом беззвучный.
 */
object Notifier {
    const val CH_GOALS = "goals_sound_v5"
    const val CH_GOALS_SILENT = "goals_silent_v1"
    const val CH_QUOTES = "quotes_sound_v2"
    const val CH_QUOTES_SILENT = "quotes_silent_v1"
    const val CH_ROUTINES = "routines_sound_v2"
    const val CH_ROUTINES_SILENT = "routines_silent_v1"
    const val CH_NOTES = "notes_sound_v2"
    const val CH_NOTES_SILENT = "notes_silent_v1"
    private val OLD_CHANNELS = listOf(
        "reminders", "goals_sound_v1", "goals_sound_v2", "goals_sound_v3", "goals_sound_v4", "quotes_v1", "routines_v1",
        "quotes_sound_v1", "routines_sound_v1", "notes_sound_v1"
    )

    private const val ID_GOALS = 1
    private const val ID_QUOTES = 2
    private const val ID_EVENING = 3

    const val TAB_PATH = 0
    const val TAB_GOALS = 1
    const val TAB_QUOTES = 2
    const val TAB_NOTES = 3

    private fun soundAttrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        OLD_CHANNELS.forEach { nm.deleteNotificationChannel(it) }
        val uri = Melody.welcomeUri(ctx)

        fun withSound(id: String, name: String, desc: String) =
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = desc
                setSound(uri, soundAttrs())
                enableVibration(true)
            }

        fun silent(id: String, name: String, desc: String) =
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = desc
                setSound(null, null)
                enableVibration(true)
            }

        nm.createNotificationChannels(
            listOf(
                withSound(CH_GOALS, "Цели", "Напоминания о целях, мелодия самурая"),
                silent(CH_GOALS_SILENT, "Цели без мелодии", "Цели: вибрация или своя мелодия острова"),
                withSound(CH_QUOTES, "Цитаты", "Цитаты, мелодия самурая"),
                silent(CH_QUOTES_SILENT, "Цитаты без мелодии", "Цитаты: вибрация или своя мелодия острова"),
                withSound(CH_ROUTINES, "Рутины", "Фонари рутин, мелодия самурая"),
                silent(CH_ROUTINES_SILENT, "Рутины без мелодии", "Рутины: вибрация или своя мелодия острова"),
                withSound(CH_NOTES, "Заметки", "Заметки по датам, мелодия самурая"),
                silent(CH_NOTES_SILENT, "Заметки без мелодии", "Заметки: вибрация или своя мелодия острова")
            )
        )
    }

    private fun channelFor(fn: String, sound: String): String {
        val loud = sound == "welcome"
        return when (fn) {
            "goals" -> if (loud) CH_GOALS else CH_GOALS_SILENT
            "quotes" -> if (loud) CH_QUOTES else CH_QUOTES_SILENT
            "routines" -> if (loud) CH_ROUTINES else CH_ROUTINES_SILENT
            else -> if (loud) CH_NOTES else CH_NOTES_SILENT
        }
    }

    private fun openApp(ctx: Context, tab: Int): PendingIntent = PendingIntent.getActivity(
        ctx, 100 + tab, Intent(ctx, MainActivity::class.java).putExtra("tab", tab),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun builder(ctx: Context, fn: String, tab: Int): Notification.Builder {
        ensureChannels(ctx)
        val snd = Store(ctx).sound(fn)
        return Notification.Builder(ctx, channelFor(fn, snd))
            .setSmallIcon(R.drawable.ic_notify)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(ctx, tab))
            .setAutoCancel(true)
    }

    /** После показа уведомления: если выбрана мелодия острова, играем её. Вызывать из фонового потока. */
    fun playCustom(ctx: Context, fn: String) {
        val store = Store(ctx)
        val snd = store.sound(fn)
        if (!snd.startsWith("m:")) return
        val id = snd.substring(2).toLongOrNull() ?: return
        val info = store.melodies().firstOrNull { it.id == id } ?: return
        Melody.playBlocking(ctx, info)
    }

    // ---------- Цели и цитаты ----------

    /** true, если уведомление показано. */
    fun showItems(ctx: Context, kind: String): Boolean {
        val store = Store(ctx)
        val items = store.load(kind).filter { it.enabled }
        if (items.isEmpty()) return false
        val nm = ctx.getSystemService(NotificationManager::class.java)

        fun fmt(i: Item): String =
            if (kind == "quotes") "«${i.text}»" + if (i.author.isNotBlank()) " — ${i.author}" else ""
            else i.text

        val body = when (store.mode(kind)) {
            "seq" -> fmt(items[store.nextIndex(kind, items.size)])
            "random" -> fmt(items.random())
            else -> items.joinToString("\n") { "• " + fmt(it) }
        }
        val tab = if (kind == "goals") TAB_GOALS else TAB_QUOTES
        val n = builder(ctx, kind, tab)
            .setContentTitle(if (kind == "goals") "Мои цели" else "Цитата")
            .setContentText(body.lines().first())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .build()
        nm.notify(if (kind == "goals") ID_GOALS else ID_QUOTES, n)
        return true
    }

    /** Проверка звука: уведомление приходит всегда, даже если список пуст. */
    fun showTest(ctx: Context, fn: String): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val title = when (fn) {
            "goals" -> "Проверка: цели"
            "quotes" -> "Проверка: цитаты"
            "routines" -> "Проверка: рутины"
            else -> "Проверка: заметки"
        }
        val n = builder(ctx, fn, TAB_PATH)
            .setContentTitle(title)
            .setContentText("Так звучит уведомление.")
            .build()
        nm.notify(30_000 + fn.hashCode() % 1000, n)
        return true
    }

    // ---------- Заметки ----------

    fun noteNotifId(id: Long): Int = 20_000 + (id % 100_000).toInt()

    fun showNote(ctx: Context, id: Long): Boolean {
        val note = Store(ctx).notes().firstOrNull { it.id == id } ?: return false
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val n = builder(ctx, "notes", TAB_NOTES)
            .setContentTitle("Заметка")
            .setContentText(note.text.lines().first())
            .setStyle(Notification.BigTextStyle().bigText(note.text))
            .build()
        nm.notify(noteNotifId(id), n)
        return true
    }

    // ---------- Рутины ----------

    fun routineNotifId(id: Long): Int = 10_000 + (id % 100_000).toInt()

    fun showRoutine(ctx: Context, id: Long): Boolean {
        val store = Store(ctx)
        val r = store.routineMetas().firstOrNull { it.id == id } ?: return false
        if (r.doneToday) return false
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
        val text = "Зажги фонарь. Масло догорит к утру."
        val n = builder(ctx, "routines", TAB_PATH)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .addAction(Notification.Action.Builder(icon, "Сделано", done).build())
            .addAction(Notification.Action.Builder(icon, "Через час", snooze).build())
            .build()
        nm.notify(nid, n)
        return true
    }

    /** Вечером: какие фонари ещё не зажжены. */
    fun showEvening(ctx: Context): Boolean {
        val store = Store(ctx)
        if (!store.eveningReminder) return false
        val left = store.routineMetas().filter { !it.doneToday }
        if (left.isEmpty()) return false
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val body = "Не зажжены: " + left.joinToString(", ") { it.title } + ". Масло догорит к утру."
        val n = builder(ctx, "routines", TAB_PATH)
            .setContentTitle("Фонари ждут")
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .build()
        nm.notify(ID_EVENING, n)
        return true
    }

    fun cancel(ctx: Context, notifId: Int) {
        ctx.getSystemService(NotificationManager::class.java).cancel(notifId)
    }
}
