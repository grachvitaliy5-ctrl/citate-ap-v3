package com.example.goals

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {
    private val kinds = listOf("goals", "quotes")
    private const val EVENING_CODE = 50
    private const val EVENING_TIME = 21 * 60

    /** Ближайший момент указанного времени (сегодня или завтра). Как в первой версии. */
    private fun nextTime(minutes: Int): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** Тот же способ постановки будильника, что и в первой версии. */
    private fun setAlarm(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun broadcast(ctx: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, code, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun routineCode(id: Long): Int = 1000 + (id % 100_000).toInt()
    private fun snoozeCode(id: Long): Int = 200_000 + (id % 100_000).toInt()

    private fun routineIntent(ctx: Context, id: Long) =
        Intent(ctx, AlarmReceiver::class.java).putExtra("kind", "routine").putExtra("id", id)

    fun scheduleAll(ctx: Context) {
        val store = Store(ctx)

        // Цели и цитаты: 4 слота, коды 0..3 (как в первой версии)
        kinds.forEachIndexed { ki, kind ->
            for (slot in 0..1) {
                val pi = broadcast(
                    ctx, ki * 2 + slot,
                    Intent(ctx, AlarmReceiver::class.java).putExtra("kind", kind)
                )
                setAlarm(ctx, nextTime(store.time(kind, slot)), pi)
            }
        }

        // Рутины
        val routines = store.routines()
        routines.forEach { r ->
            setAlarm(ctx, nextTime(r.time), broadcast(ctx, routineCode(r.id), routineIntent(ctx, r.id)))
        }

        // Вечерняя проверка в 21:00
        if (routines.isNotEmpty()) {
            setAlarm(
                ctx, nextTime(EVENING_TIME),
                broadcast(ctx, EVENING_CODE, Intent(ctx, AlarmReceiver::class.java).putExtra("kind", "evening"))
            )
        }
    }

    fun cancelRoutine(ctx: Context, id: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(broadcast(ctx, routineCode(id), routineIntent(ctx, id)))
        am.cancel(broadcast(ctx, snoozeCode(id), routineIntent(ctx, id)))
        Notifier.cancel(ctx, Notifier.routineNotifId(id))
    }

    fun snooze(ctx: Context, id: Long) {
        val at = System.currentTimeMillis() + 60 * 60 * 1000L
        setAlarm(ctx, at, broadcast(ctx, snoozeCode(id), routineIntent(ctx, id)))
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind: String = intent.getStringExtra("kind") ?: ""
        when (kind) {
            "goals", "quotes" -> Notifier.show(ctx, kind)
            "routine" -> {
                val id = intent.getLongExtra("id", -1L)
                if (id >= 0) Notifier.showRoutine(ctx, id)
            }
            "evening" -> Notifier.showEvening(ctx)
        }
        Scheduler.scheduleAll(ctx) // ставим следующий день
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
            ACTION_DONE -> Store(ctx).markToday(id)
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
