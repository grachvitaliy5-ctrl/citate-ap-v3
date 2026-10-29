package com.example.goals

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

data class Item(val id: Long, val text: String, val author: String, val enabled: Boolean)

/**
 * Напоминание цели или цитаты. Их может быть сколько угодно.
 * interval=false: каждый день в time. interval=true: каждые everyMin минут между from и to.
 * Время хранится в минутах от полуночи.
 */
data class Reminder(
    val id: Long,
    val interval: Boolean,
    val time: Int,
    val everyMin: Int,
    val from: Int,
    val to: Int,
    val enabled: Boolean
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("iv", interval).put("t", time)
        .put("e", everyMin).put("f", from).put("to", to).put("on", enabled)

    fun describe(): String =
        if (interval) "Каждые ${everyLabel(everyMin)}, ${hhmm(from)}–${hhmm(to)}"
        else "Каждый день в ${hhmm(time)}"

    companion object {
        fun fromJson(o: JSONObject) = Reminder(
            o.getLong("id"), o.optBoolean("iv", false), o.optInt("t", 540),
            o.optInt("e", 60), o.optInt("f", 540), o.optInt("to", 1260), o.optBoolean("on", true)
        )
    }
}

fun everyLabel(m: Int): String = when {
    m % 60 == 0 -> "${m / 60} ч"
    m > 60 -> "${m / 60} ч ${m % 60} мин"
    else -> "$m мин"
}

/** Заметка, привязанная к дате календаря. time = -1: без напоминания. */
data class Note(val id: Long, val day: Long, val text: String, val time: Int)

/** Рутина из сцены «Путь»: нужна уведомлениям. Сама сцена хранит всё остальное. */
data class RoutineMeta(val id: Long, val title: String, val time: Int, val remind: Boolean, val doneToday: Boolean)

/**
 * Хранилище на SharedPreferences + JSON.
 * Ключи целей и цитат не менялись с первой версии, данные сохраняются.
 */
class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE)

    companion object {
        private val lock = Any()
        private val LCOLS = listOf("#ff5a7a", "#ffb347", "#ffe066", "#7ee081", "#5ad1ff", "#7a8cff", "#c77dff", "#f4f0e8")
    }

    // ---------- Цели и цитаты ----------

    fun load(kind: String): List<Item> {
        val arr = JSONArray(p.getString(kind, "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Item(o.getLong("id"), o.getString("text"), o.optString("author"), o.optBoolean("on", true))
        }
    }

    fun save(kind: String, items: List<Item>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("text", it.text)
                    .put("author", it.author).put("on", it.enabled)
            )
        }
        p.edit().putString(kind, arr.toString()).apply()
    }

    /** Время в минутах от полуночи из первой версии (slot: 0 или 1). Нужно только для переноса. */
    private fun oldTime(kind: String, slot: Int): Int =
        p.getInt("t_${kind}_$slot", if (slot == 0) 9 * 60 else 20 * 60)

    fun reminders(kind: String): List<Reminder> {
        val raw = p.getString("rem_$kind", null)
        if (raw == null) {
            val migrated = listOf(
                Reminder(1, false, oldTime(kind, 0), 60, 9 * 60, 21 * 60, true),
                Reminder(2, false, oldTime(kind, 1), 60, 9 * 60, 21 * 60, true)
            )
            saveReminders(kind, migrated)
            return migrated
        }
        val arr = JSONArray(raw)
        return List(arr.length()) { Reminder.fromJson(arr.getJSONObject(it)) }
    }

    fun saveReminders(kind: String, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        p.edit().putString("rem_$kind", arr.toString()).commit()
    }

    /** all / seq / random */
    fun mode(kind: String): String =
        p.getString("mode_$kind", if (kind == "goals") "all" else "random")!!

    fun setMode(kind: String, mode: String) =
        p.edit().putString("mode_$kind", mode).apply()

    fun nextIndex(kind: String, size: Int): Int {
        val i = p.getInt("idx_$kind", 0) % size
        p.edit().putInt("idx_$kind", i + 1).apply()
        return i
    }

    // ---------- Звук по функциям ----------

    /** fn: goals, quotes, routines, notes. Значение: welcome, silent или m:<id мелодии>. */
    fun sound(fn: String): String {
        val v = p.getString("snd_$fn", null)
        if (v != null) return v
        if (fn == "goals" && !p.getBoolean("goal_sound", true)) return "silent"
        return "welcome"
    }

    fun setSound(fn: String, v: String) = p.edit().putString("snd_$fn", v).apply()

    /** Чёткость сцены: 0 — пиксельно, 1 — средне, 2 — чётко. */
    var sceneQuality: Int
        get() = p.getInt("scene_quality", 1)
        set(v) {
            p.edit().putInt("scene_quality", v).apply()
        }

    /** Случайная включённая цитата для слов самурая. */
    fun randomQuote(): String {
        val q = load("quotes").filter { it.enabled }
        if (q.isEmpty()) return ""
        val i = q.random()
        return "«${i.text}»" + if (i.author.isNotBlank()) "\n— ${i.author}" else ""
    }

    // ---------- Настройки рутин ----------

    /** Час начала нового дня для рутин: 0 или 4 */
    var dayStartHour: Int
        get() = p.getInt("day_start", 0)
        set(v) {
            p.edit().putInt("day_start", v).apply()
        }

    var eveningReminder: Boolean
        get() = p.getBoolean("evening", true)
        set(v) {
            p.edit().putBoolean("evening", v).apply()
        }

    /** Текущий день (epochDay) с учётом сдвига начала дня. */
    fun today(): Long =
        LocalDateTime.now().minusHours(dayStartHour.toLong()).toLocalDate().toEpochDay()

    // ---------- Заметки ----------

    fun notes(): List<Note> {
        val arr = JSONArray(p.getString("notes", "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Note(o.getLong("id"), o.getLong("day"), o.getString("text"), o.optInt("time", -1))
        }
    }

    fun saveNotes(list: List<Note>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("day", it.day).put("text", it.text).put("time", it.time))
        }
        p.edit().putString("notes", arr.toString()).commit()
    }

    fun todayCalendar(): Long = LocalDate.now().toEpochDay()

    // ---------- Сцена «Путь» ----------

    /** Состояние сцены (JSON). При первом запуске переносит рутины из прежней версии. */
    fun pathState(): String {
        val s = p.getString("path_state", null)
        if (!s.isNullOrBlank()) return s
        return seedFromOldRoutines()
    }

    fun setPathState(json: String) {
        p.edit().putString("path_state", json).commit()
    }

    private fun seedFromOldRoutines(): String {
        val old = try {
            JSONArray(p.getString("routines", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        if (old.length() == 0) return ""
        val arr = JSONArray()
        for (k in 0 until minOf(6, old.length())) {
            val o = old.getJSONObject(k)
            arr.put(
                JSONObject().put("id", k).put("t", o.optString("title", "Рутина").take(18))
                    .put("type", "other").put("color", LCOLS[k % LCOLS.size]).put("shape", k % 6)
                    .put("d", false).put("st", "g").put("streak", 0)
                    .put("time", o.optInt("time", 480)).put("rem", true)
            )
        }
        return JSONObject().put("v", 1).put("uid", arr.length()).put("routines", arr).toString()
    }

    fun routineMetas(): List<RoutineMeta> {
        val s = pathState()
        if (s.isBlank()) return emptyList()
        return try {
            val o = JSONObject(s)
            val arr = o.optJSONArray("routines") ?: return emptyList()
            val today = today()
            val lastDay = if (o.isNull("lastDay")) -1L else o.optLong("lastDay", -1L)
            List(arr.length()) {
                val r = arr.getJSONObject(it)
                val id = r.getLong("id")
                RoutineMeta(
                    id, r.optString("t", "Рутина"), r.optInt("time", 480), r.optBoolean("rem", false),
                    (lastDay == today && r.optBoolean("d", false)) || hasPending(id, today)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun melodies(): List<Melody.Info> = Melody.parseCollection(pathState())

    // Отметки «Сделано» из уведомлений. Сцена применит их при открытии.

    fun pendingJson(): String = synchronized(lock) { p.getString("pending", "[]") ?: "[]" }

    fun addPending(id: Long, day: Long) = synchronized(lock) {
        val arr = JSONArray(p.getString("pending", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("id") == id && o.getLong("day") == day) return@synchronized
        }
        arr.put(JSONObject().put("id", id).put("day", day))
        p.edit().putString("pending", arr.toString()).commit()
        Unit
    }

    fun ack(id: Long, day: Long) = synchronized(lock) {
        val arr = JSONArray(p.getString("pending", "[]"))
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!(o.getLong("id") == id && o.getLong("day") == day)) out.put(o)
        }
        p.edit().putString("pending", out.toString()).commit()
        Unit
    }

    fun hasPending(id: Long, day: Long): Boolean = synchronized(lock) {
        val arr = JSONArray(p.getString("pending", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("id") == id && o.getLong("day") == day) return@synchronized true
        }
        false
    }
}
