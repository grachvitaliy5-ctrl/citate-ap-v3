package com.example.goals

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

/**
 * Будильники ставятся для каждого напоминания отдельно, их число не ограничено.
 * Каждый будильник опознаётся по адресу goalsquotes://alarm/<вид>/<id>.
 * При любом изменении все будильники пересоздаются: scheduleAll сначала снимает
 * прежние (их ключи хранятся в prefs "sched"), потом ставит актуальные.
 */
object Scheduler {
    private const val PREF = "sched"
    private const val EVENING_TIME = 21 * 60

    private class Job(val key: String, val kind: String, val id: Long, val at: Long)

    private fun intentFor(ctx: Context, kind: String, id: Long): Intent =
        Intent(ctx, AlarmReceiver::class.java)
            .setData(Uri.parse("goalsquotes://alarm/$kind/$id"))
            .putExtra("kind", kind)
            .putExtra("id", id)

    private fun pendingFor(ctx: Context, kind: String, id: Long, create: Boolean): PendingIntent? {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(ctx, 0, intentFor(ctx, kind, id), flags)
    }

    private fun startOfToday(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** Ближайший момент указанного времени (сегодня или завтра). */
    private fun nextTime(minutes: Int): Long {
        val c = startOfToday()
        c.add(Calendar.MINUTE, minutes)
        if (c.timeInMillis <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun nextFor(r: Reminder): Long {
        if (!r.interval) return nextTime(r.time)
        val step = maxOf(1, r.everyMin)
        val now = System.currentTimeMillis() + 1000
        val day = startOfToday().timeInMillis
        var m = r.from
        while (m <= r.to) {
            val at = day + m * 60_000L
            if (at > now) return at
            m += step
        }
        return nextTime(r.from)
    }

    private fun noteTime(n: Note): Long =
        LocalDate.ofEpochDay(n.day).atTime(n.time / 60, n.time % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    @Synchronized
    fun scheduleAll(ctx: Context) {
        val app = ctx.applicationContext
        val prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val am = app.getSystemService(AlarmManager::class.java)

        // 1. снять прежние
        val old = prefs.getStringSet("keys", emptySet()) ?: emptySet()
        for (key in old) {
            val parts = key.split("/")
            if (parts.size != 2) continue
            val id = parts[1].toLongOrNull() ?: continue
            val pi = pendingFor(app, parts[0], id, false)
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }

        // 2. собрать актуальные
        val store = Store(app)
        val jobs = ArrayList<Job>()
        for (kind in listOf("goals", "quotes")) {
            store.reminders(kind).filter { it.enabled }.forEach { r ->
                jobs.add(Job("$kind/${r.id}", kind, r.id, nextFor(r)))
            }
        }
        val metas = store.routineMetas()
        metas.filter { it.remind }.forEach { jobs.add(Job("routine/${it.id}", "routine", it.id, nextTime(it.time))) }
        if (metas.isNotEmpty() && store.eveningReminder) {
            jobs.add(Job("evening/0", "evening", 0L, nextTime(EVENING_TIME)))
        }
        val now = System.currentTimeMillis()
        store.notes().filter { it.time >= 0 }.forEach { n ->
            val at = noteTime(n)
            if (at > now + 1000) jobs.add(Job("note/${n.id}", "note", n.id, at))
        }

        // 3. поставить
        val keys = HashSet<String>()
        for (j in jobs) {
            val pi = pendingFor(app, j.kind, j.id, true) ?: continue
            setAlarm(app, j.at, pi)
            keys.add(j.key)
        }
        prefs.edit().putStringSet("keys", keys).commit()
    }

    fun cancelRoutine(ctx: Context, id: Long) {
        val app = ctx.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        for (kind in listOf("routine", "snooze")) {
            val pi = pendingFor(app, kind, id, false)
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
        Notifier.cancel(app, Notifier.routineNotifId(id))
    }

    fun snooze(ctx: Context, id: Long) {
        val app = ctx.applicationContext
        val at = System.currentTimeMillis() + 60 * 60 * 1000L
        val pi = pendingFor(app, "snooze", id, true) ?: return
        setAlarm(app, at, pi)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val id = intent.getLongExtra("id", -1L)
        val app = ctx.applicationContext
        val pending = goAsync()
        Thread {
            try {
                var fn = ""
                var posted = false
                try {
                    when (kind) {
                        "goals", "quotes" -> {
                            fn = kind
                            posted = Notifier.showItems(app, kind)
                        }
                        "note" -> {
                            fn = "notes"
                            posted = Notifier.showNote(app, id)
                        }
                        "routine", "snooze" -> {
                            fn = "routines"
                            if (id >= 0) posted = Notifier.showRoutine(app, id)
                        }
                        "evening" -> {
                            fn = "routines"
                            posted = Notifier.showEvening(app)
                        }
                    }
                } catch (e: Exception) {
                }
                // следующий будильник ставим до воспроизведения мелодии
                try {
                    Scheduler.scheduleAll(app)
                } catch (e: Exception) {
                }
                if (posted) {
                    try {
                        Notifier.playCustom(app, fn)
                    } catch (e: Exception) {
                    }
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}

class RoutineActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.example.goals.ROUTINE_DONE"
        const val ACTION_SNOOZE = "com.example.goals.ROUTINE_SNOOZE"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id < 0) return
        when (intent.action) {
            ACTION_DONE -> {
                val store = Store(ctx)
                store.addPending(id, store.today())
            }
            ACTION_SNOOZE -> Scheduler.snooze(ctx, id)
        }
        Notifier.cancel(ctx, Notifier.routineNotifId(id))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.scheduleAll(ctx)
    }
}
