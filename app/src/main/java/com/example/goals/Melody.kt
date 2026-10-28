package com.example.goals

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Мелодии островов из сцены «Путь»: список нот превращается в звук флейты прямо в телефоне.
 * Алгоритм флейты тот же, что в tools/generate_welcome.py.
 */
object Melody {
    class N(val i: Int, val dur: Double, val bad: Boolean)

    class Info(val id: Long, val num: Int, val bad: Boolean, val level: Int, val notes: List<N>) {
        fun title(): String = "Остров ${roman(num)}" + if (bad) " (фальшивая)" else ""
    }

    private val SC = doubleArrayOf(
        293.66, 349.23, 392.0, 440.0, 523.25, 587.33, 698.46, 783.99, 880.0, 1046.5, 1174.66
    )
    private const val SR = 22050

    fun roman(n: Int): String {
        val table = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        var rest = n
        val sb = StringBuilder()
        for ((v, s) in table) {
            while (rest >= v) {
                sb.append(s)
                rest -= v
            }
        }
        return sb.toString()
    }

    fun parseCollection(stateJson: String): List<Info> {
        if (stateJson.isBlank()) return emptyList()
        return try {
            val arr = JSONObject(stateJson).optJSONArray("collection") ?: return emptyList()
            List(arr.length()) { k ->
                val c = arr.getJSONObject(k)
                val na = c.optJSONArray("notes") ?: JSONArray()
                val notes = List(na.length()) { j ->
                    val n = na.getJSONObject(j)
                    N(n.optInt("i", 0).coerceIn(0, SC.size - 1), n.optDouble("dur", 1.0), n.optBoolean("bad", false))
                }
                Info(c.optLong("id", k.toLong()), c.optInt("i", k + 1), c.optBoolean("bad", false), c.optInt("L", 0), notes)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun compress(notes: List<N>): List<N> {
        if (notes.size <= 32) return notes
        val out = ArrayList<N>(notes.subList(0, 8))
        val mid = notes.subList(8, notes.size - 8)
        val k = mid.size / 16.0
        for (q in 0 until 16) out.add(mid[floor(q * k).toInt()])
        out.addAll(notes.subList(notes.size - 8, notes.size))
        return out
    }

    private fun envelope(t: Double, dur: Double): Double {
        val t2 = max(0.06, dur - 0.08)
        return when {
            t < 0.05 -> 0.2 * t / 0.05
            t < t2 -> 0.2 - 0.04 * (t - 0.05) / (t2 - 0.05)
            t < dur + 0.14 -> 0.16 * (1.0 - (t - t2) / (dur + 0.14 - t2))
            else -> 0.0
        }
    }

    private fun addFlute(buf: DoubleArray, start: Double, f: Double, dur: Double, vel: Double, seed: Int) {
        val s0 = (start * SR).toInt()
        val n = ((dur + 0.45) * SR).toInt()
        var ph = 0.0
        var rnd = (seed * 7919L + 17L)
        var lp = 0.0
        val a = 1.0 - exp(-2.0 * PI * (f * 2.0) / SR)
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= buf.size) break
            val t = i.toDouble() / SR
            var fr = if (t < 0.07) f * (0.97 + 0.03 * t / 0.07) else f
            fr += f * 0.007 * min(1.0, t / 0.3) * sin(2.0 * PI * 5.6 * t)
            ph += 2.0 * PI * fr / SR
            val tri = 2.0 / PI * asin(sin(ph))
            val osc = tri + 0.05 * (sin(ph) + sin(3.0 * ph) / 3.0)
            rnd = (rnd * 1103515245L + 12345L) and 0x7fffffffL
            val white = rnd.toDouble() / 0x7fffffffL * 2.0 - 1.0
            lp += (white - lp) * a
            val nenv = when {
                t < 0.02 -> 0.5 * t / 0.02
                t < 0.14 -> 0.5 - 0.42 * (t - 0.02) / 0.12
                t < dur -> 0.08 * (1.0 - (t - 0.14) / max(0.01, dur - 0.14))
                else -> 0.0
            }
            buf[idx] += (osc + 0.32 * lp * nenv) * envelope(t, dur) * vel
        }
    }

    private fun addDrone(buf: DoubleArray, start: Double, f: Double, dur: Double, amp: Double) {
        val s0 = (start * SR).toInt()
        val n = (dur * SR).toInt()
        var ph = 0.0
        for (i in 0 until n) {
            val idx = s0 + i
            if (idx >= buf.size) break
            val t = i.toDouble() / SR
            ph += 2.0 * PI * f / SR
            buf[idx] += amp * 2.0 / PI * asin(sin(ph)) * min(1.0, t / 0.2) * exp(-t / 1.6)
        }
    }

    private fun echo(x: DoubleArray): DoubleArray {
        val d = (0.33 * SR).toInt()
        val wet = DoubleArray(x.size)
        var lp = 0.0
        for (i in d until x.size) {
            val src = wet[i - d] * 0.34 + x[i - d]
            lp += (src - lp) * 0.6
            wet[i] = lp
        }
        return DoubleArray(x.size) { x[it] + 0.45 * wet[it] }
    }

    /** Звук мелодии: 16-битный моно PCM, 22050 Гц. */
    fun render(src: List<N>): ShortArray {
        val notes = compress(src)
        if (notes.isEmpty()) return ShortArray(0)
        val beat = 0.36
        var total = 0.0
        notes.forEach { total += it.dur * beat }
        val bad = notes.any { it.bad }
        val buf = DoubleArray(((total + beat * 3 + 1.8) * SR).toInt())
        var acc = 0.0
        notes.forEachIndexed { k, n ->
            val f = SC[n.i]
            if (n.bad) addFlute(buf, acc, f * 1.059, n.dur * beat * 1.05, 0.8, k)
            else addFlute(buf, acc, f, n.dur * beat * 1.05, 0.95, k)
            acc += n.dur * beat
        }
        val tonic = if (notes.last().i >= 3) 5 else 0
        if (bad) {
            addFlute(buf, acc, SC[tonic] * 1.059, beat * 3, 0.8, 99)
        } else {
            addFlute(buf, acc, SC[tonic], beat * 3, 1.0, 99)
            addDrone(buf, acc, 73.42, 2.8, 0.12)
        }
        val mixed = echo(buf)
        var peak = 0.0
        for (v in mixed) peak = max(peak, kotlin.math.abs(v))
        val g = if (peak > 0.0) 0.8 / peak else 1.0
        val fade = (0.4 * SR).toInt()
        return ShortArray(mixed.size) { i ->
            var v = mixed[i] * g
            val left = mixed.size - i
            if (left < fade) v *= left.toDouble() / fade
            (v * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun soundAllowed(ctx: Context): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java)
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    /** Играет мелодию и ждёт конца. Вызывать из фонового потока. */
    fun playBlocking(ctx: Context, info: Info, force: Boolean = false) {
        if (!force && !soundAllowed(ctx)) return
        val pcm = render(info.notes)
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(pcm.size * 1000L / SR + 250L)
        } finally {
            track.release()
        }
    }

    fun play(ctx: Context, info: Info) {
        val app = ctx.applicationContext
        Thread {
            try {
                playBlocking(app, info, true)
            } catch (e: Exception) {
            }
        }.start()
    }

    fun welcomeUri(ctx: Context): Uri =
        Uri.parse("android.resource://${ctx.packageName}/${R.raw.samurai_welcome}")

    /** Прослушать мелодию приветствия самурая (она же звук по умолчанию). */
    fun previewWelcome(ctx: Context) {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(ctx, welcomeUri(ctx))
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
