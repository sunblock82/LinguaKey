package com.linguakey.keyboard

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

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
            else -> "c1c2"
        }
        p.edit()
            .putInt(key, p.getInt(key, 0) + 1)
            .putInt("today_translated", p.getInt("today_translated", 0) + 1)
            .putInt(bucket, p.getInt(bucket, 0) + 1)
            .apply()
    }

    private fun touchDay() {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val last = p.getLong("last_day", Long.MIN_VALUE)
        if (last == today) return
        val streak = if (last == today - 1) p.getInt("streak", 0) + 1 else 1
        p.edit().putLong("last_day", today).putInt("streak", streak).putInt("today_translated", 0).apply()
    }

    fun snapshot(): Snapshot {
        touchDay()
        return Snapshot(
            p.getInt("translated", 0), p.getInt("listened", 0), p.getInt("saved", 0),
            p.getInt("reviewed", 0), p.getInt("streak", 0), p.getInt("today_translated", 0),
            p.getInt("a1a2", 0), p.getInt("b1b2", 0), p.getInt("c1c2", 0)
        )
    }
}
