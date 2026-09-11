package com.linguakey.keyboard

import android.content.Context
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class LearningStats(context: Context) {
    private val p = context.getSharedPreferences("learning_stats", Context.MODE_PRIVATE)

    data class Snapshot(
        val translated: Int,
        val listened: Int,
        val saved: Int,
        val reviewed: Int,
        val streak: Int,
        val todayTranslated: Int,
        val a1a2: Int,
        val b1b2: Int,
        val c1c2: Int
    )

    fun recordTranslation(level: String) = bump("translated", level)
    fun recordListen() = bumpSimple("listened")
    fun recordSave() = bumpSimple("saved")
    fun recordReview() = bumpSimple("reviewed")

    private fun bumpSimple(key: String) {
        touchDay()
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
    }

    private fun bump(key: String, level: String) {
        touchDay()
        val bucket = when (level.firstOrNull()?.uppercaseChar()) {
            'A' -> "a1a2"
            'B' -> "b1b2"
            'C' -> "c1c2"
            else -> null
        }
        val edit = p.edit().putInt(key, p.getInt(key, 0) + 1)
            .putInt("today_translated", p.getInt("today_translated", 0) + 1)
        if (bucket != null) edit.putInt(bucket, p.getInt(bucket, 0) + 1)
        edit.apply()
    }

    private fun touchDay() {
        val today = today()
        val last = p.getLong("last_day", Long.MIN_VALUE)
        if (last == today) return
        val streak = if (last == today - 1) p.getInt("streak", 0) + 1 else 1
        p.edit().putLong("last_day", today).putInt("streak", streak).putInt("today_translated", 0).apply()
    }

    private fun today(): Long {
        val local = Calendar.getInstance()
        val utc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        return utc.timeInMillis / 86_400_000L
    }

    fun snapshot(): Snapshot {
        val last = p.getLong("last_day", Long.MIN_VALUE)
        val now = today()
        return Snapshot(
            p.getInt("translated", 0), p.getInt("listened", 0), p.getInt("saved", 0),
            p.getInt("reviewed", 0), if (last == now || last == now-1) p.getInt("streak", 0) else 0, if (last == now) p.getInt("today_translated", 0) else 0,
            p.getInt("a1a2", 0), p.getInt("b1b2", 0), p.getInt("c1c2", 0)
        )
    }
}
