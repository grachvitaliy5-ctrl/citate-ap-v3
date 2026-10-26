package com.example.goals

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

data class Item(val id: Long, val text: String, val author: String, val enabled: Boolean)

/**
 * Хранилище на SharedPreferences + JSON.
 * Ключи целей и цитат не изменились с первой версии, данные сохраняются.
 */
class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE)

    // ---------- Цели и цитаты (как в первой версии) ----------

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

    /** Время в минутах от полуночи. slot: 0 или 1 */
    fun time(kind: String, slot: Int): Int =
        p.getInt("t_${kind}_$slot", if (slot == 0) 9 * 60 else 20 * 60)

    fun setTime(kind: String, slot: Int, minutes: Int) =
        p.edit().putInt("t_${kind}_$slot", minutes).apply()

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

    // ---------- Настройки ----------

    var goalSound: Boolean
        get() = p.getBoolean("goal_sound", true)
        set(v) {
            p.edit().putBoolean("goal_sound", v).apply()
        }

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

    var weeklySummary: Boolean
        get() = p.getBoolean("weekly", true)
        set(v) {
            p.edit().putBoolean("weekly", v).apply()
        }

    /** Текущий день (epochDay) с учётом сдвига начала дня. */
    fun today(): Long =
        LocalDateTime.now().minusHours(dayStartHour.toLong()).toLocalDate().toEpochDay()

    // ---------- Рутины ----------

    fun routines(): List<Routine> {
        val arr = JSONArray(p.getString("routines", "[]"))
        return List(arr.length()) { Routine.fromJson(arr.getJSONObject(it)) }
    }

    private fun saveRoutines(list: List<Routine>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        p.edit().putString("routines", arr.toString()).commit()
    }

    /** Применяет сбросы (красный статус) и возвращает актуальный список. */
    fun refreshRoutines(): List<Routine> {
        val t = today()
        val list = routines()
        val updated = list.map { RoutineLogic.applyReset(it, t) }
        if (updated != list) saveRoutines(updated)
        return updated
    }

    fun addRoutine(title: String, cycle: Int, time: Int): List<Routine> {
        val list = refreshRoutines() + Routine.create(title, cycle, time, today())
        saveRoutines(list)
        return list
    }

    fun updateRoutine(id: Long, f: (Routine) -> Routine): List<Routine> {
        val list = refreshRoutines().map { if (it.id == id) f(it) else it }
        saveRoutines(list)
        return list
    }

    fun deleteRoutine(id: Long): List<Routine> {
        val list = refreshRoutines().filter { it.id != id }
        saveRoutines(list)
        return list
    }

    fun markToday(id: Long): List<Routine> {
        val t = today()
        return updateRoutine(id) { RoutineLogic.markDay(it, t) }
    }
}
