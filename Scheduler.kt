package com.example.goals

import org.json.JSONArray
import org.json.JSONObject

/** Завершённая попытка (сброс или пройденный цикл). */
data class Attempt(val number: Int, val days: Int, val endDay: Long, val completed: Boolean)

/** Сброс, который пользователь ещё не увидел. */
data class PendingReset(val attempt: Int, val days: Int)

/**
 * Рутина. Дни хранятся как epochDay (LocalDate.toEpochDay()).
 * startDay — первый день текущего цикла.
 */
data class Routine(
    val id: Long,
    val title: String,
    val cycleLength: Int,
    val time: Int,
    val startDay: Long,
    val completedDays: List<Long>,
    val bestStreak: Int,
    val resets: Int,
    val history: List<Attempt>,
    val pendingReset: PendingReset?
) {
    val attemptNumber: Int get() = history.size + 1
    val progress: Int get() = completedDays.size
    val finished: Boolean get() = progress >= cycleLength
    fun doneOn(day: Long): Boolean = completedDays.contains(day)

    fun toJson(): JSONObject {
        val done = JSONArray()
        completedDays.forEach { done.put(it) }
        val hist = JSONArray()
        history.forEach {
            hist.put(
                JSONObject().put("n", it.number).put("days", it.days)
                    .put("end", it.endDay).put("ok", it.completed)
            )
        }
        val o = JSONObject()
            .put("id", id).put("title", title).put("cycle", cycleLength).put("time", time)
            .put("start", startDay).put("done", done).put("best", bestStreak)
            .put("resets", resets).put("history", hist)
        if (pendingReset != null) {
            o.put("pending", JSONObject().put("n", pendingReset.attempt).put("days", pendingReset.days))
        }
        return o
    }

    companion object {
        fun create(title: String, cycle: Int, time: Int, today: Long) = Routine(
            id = System.currentTimeMillis(),
            title = title,
            cycleLength = cycle,
            time = time,
            startDay = today,
            completedDays = emptyList(),
            bestStreak = 0,
            resets = 0,
            history = emptyList(),
            pendingReset = null
        )

        fun fromJson(o: JSONObject): Routine {
            val doneArr = o.optJSONArray("done") ?: JSONArray()
            val done = List(doneArr.length()) { doneArr.getLong(it) }
            val histArr = o.optJSONArray("history") ?: JSONArray()
            val hist = List(histArr.length()) {
                val h = histArr.getJSONObject(it)
                Attempt(h.getInt("n"), h.getInt("days"), h.getLong("end"), h.optBoolean("ok", false))
            }
            val pend = o.optJSONObject("pending")?.let { PendingReset(it.getInt("n"), it.getInt("days")) }
            return Routine(
                id = o.getLong("id"),
                title = o.getString("title"),
                cycleLength = o.optInt("cycle", 30),
                time = o.optInt("time", 8 * 60),
                startDay = o.getLong("start"),
                completedDays = done.sorted(),
                bestStreak = o.optInt("best", 0),
                resets = o.optInt("resets", 0),
                history = hist,
                pendingReset = pend
            )
        }
    }
}

enum class Status { GREEN, YELLOW, RED }

/**
 * Состояние рутины, вычисленное из дат выполнения.
 *
 * Правила:
 *  ЗЕЛЁНЫЙ — все прошедшие дни цикла отмечены.
 *  ЖЁЛТЫЙ  — пропущен один день. Возврат в зелёный после 3 отмеченных дней подряд.
 *  КРАСНЫЙ — пропущено 2 дня подряд ИЛИ новый пропуск, пока карта жёлтая.
 *            Цикл сбрасывается.
 * Сегодняшний день не считается пропуском, пока он не закончился.
 *
 * Примеры (start = день 0, today = день 5, отмечены дни из списка):
 *  [0,1,2,3,4]     -> GREEN, серия 5
 *  [0,1,3,4]       -> YELLOW (пропуск дня 2, после него 2 отметки из 3 нужных)
 *  [0,2,3,4]       -> GREEN  (пропуск дня 1, затем 3 дня подряд)
 *  [0,1,4]         -> RED    (дни 2 и 3 пропущены подряд), resetDay = 3
 *  [0,2,4]         -> RED    (пропуск дня 1, затем пропуск дня 3 до восстановления)
 */
data class RoutineState(
    val status: Status,
    val streak: Int,
    val misses: Int,
    val recoverLeft: Int,
    val resetDay: Long?
) {
    companion object {
        private const val RECOVER_DAYS = 3

        fun compute(startDay: Long, completed: Set<Long>, today: Long): RoutineState {
            var status = Status.GREEN
            var missRun = 0
            var recover = 0
            var misses = 0
            var d = startDay
            while (d < today) {
                if (completed.contains(d)) {
                    missRun = 0
                    if (status == Status.YELLOW) {
                        recover++
                        if (recover >= RECOVER_DAYS) status = Status.GREEN
                    }
                } else {
                    misses++
                    missRun++
                    if (missRun >= 2 || status == Status.YELLOW) {
                        return RoutineState(Status.RED, 0, misses, 0, d)
                    }
                    status = Status.YELLOW
                    recover = 0
                }
                d++
            }
            if (completed.contains(today) && status == Status.YELLOW) {
                recover++
                if (recover >= RECOVER_DAYS) status = Status.GREEN
            }
            var streak = 0
            var s = if (completed.contains(today)) today else today - 1
            while (s >= startDay && completed.contains(s)) {
                streak++
                s--
            }
            val left = if (status == Status.YELLOW) RECOVER_DAYS - recover else 0
            return RoutineState(status, streak, misses, left, null)
        }
    }
}

object RoutineLogic {

    fun state(r: Routine, today: Long): RoutineState {
        val day = if (r.finished) (r.completedDays.maxOrNull() ?: today) else today
        return RoutineState.compute(r.startDay, r.completedDays.toSet(), day)
    }

    /** Если карта стала красной — архивирует попытку и начинает новый цикл с сегодняшнего дня. */
    fun applyReset(r: Routine, today: Long): Routine {
        if (r.finished) return r
        val st = RoutineState.compute(r.startDay, r.completedDays.toSet(), today)
        if (st.status != Status.RED) return r
        val attempt = Attempt(r.attemptNumber, r.progress, st.resetDay ?: today, false)
        return r.copy(
            startDay = today,
            completedDays = emptyList(),
            resets = r.resets + 1,
            history = r.history + attempt,
            pendingReset = PendingReset(r.attemptNumber, r.progress)
        )
    }

    fun markDay(r: Routine, today: Long): Routine {
        if (r.finished || r.doneOn(today)) return r.copy(pendingReset = null)
        val updated = r.copy(completedDays = (r.completedDays + today).sorted(), pendingReset = null)
        val streak = RoutineState.compute(updated.startDay, updated.completedDays.toSet(), today).streak
        return updated.copy(bestStreak = maxOf(updated.bestStreak, streak))
    }

    fun newCycle(r: Routine, today: Long): Routine {
        val hist = if (r.finished) r.history + Attempt(r.attemptNumber, r.progress, today, true) else r.history
        return r.copy(startDay = today, completedDays = emptyList(), history = hist, pendingReset = null)
    }
}

/** Склонение: 1 день, 2 дня, 5 дней. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    val w = when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
    return "$n $w"
}

fun days(n: Int) = plural(n, "день", "дня", "дней")
