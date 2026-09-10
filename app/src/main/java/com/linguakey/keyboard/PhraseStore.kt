package com.linguakey.keyboard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class PhraseStore(context: Context) {
    private val p = context.getSharedPreferences("phrases", Context.MODE_PRIVATE)

    data class Phrase(
        val id: String,
        val ko: String,
        val en: String,
        val createdAt: Long,
        val dueAt: Long,
        val repetitions: Int,
        val intervalDays: Int,
        val ease: Double,
        val lapses: Int
    )

    fun all(): MutableList<Phrase> = runCatching {
        val arr = JSONArray(p.getString("items", "[]"))
        MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Phrase(
                id = o.getString("id"),
                ko = o.getString("ko"),
                en = o.getString("en"),
                createdAt = o.getLong("createdAt"),
                dueAt = o.getLong("dueAt"),
                repetitions = o.optInt("repetitions", o.optInt("box", 0)),
                intervalDays = o.optInt("intervalDays", legacyInterval(o.optInt("box", 0))),
                ease = o.optDouble("ease", 2.5),
                lapses = o.optInt("lapses", 0)
            )
        }
    }.getOrDefault(mutableListOf())

    fun save(ko: String, en: String) {
        if (ko.isBlank() || en.isBlank()) return
        val items = all()
        if (items.any { it.ko == ko && it.en == en }) return
        val now = System.currentTimeMillis()
        items.add(0, Phrase(UUID.randomUUID().toString(), ko, en, now, now, 0, 0, 2.5, 0))
        persist(items.take(1000))
    }

    fun delete(id: String) = persist(all().filterNot { it.id == id })

    fun due(): List<Phrase> {
        val now = System.currentTimeMillis()
        return all().filter { it.dueAt <= now }.sortedBy { it.dueAt }
    }

    /** SM-2 inspired grading: 0=Again, 1=Hard, 2=Good, 3=Easy. */
    fun rate(id: String, grade: Int) {
        val list = all()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = list[index]
        val q = when (grade.coerceIn(0, 3)) { 0 -> 1; 1 -> 3; 2 -> 4; else -> 5 }

        var ease = old.ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        ease = max(1.3, ease)
        var reps = old.repetitions
        var interval = old.intervalDays
        var lapses = old.lapses

        if (q < 3) {
            reps = 0
            interval = 0
            lapses += 1
        } else {
            reps += 1
            interval = when (reps) {
                1 -> if (grade == 1) 1 else 1
                2 -> if (grade == 3) 5 else 3
                else -> max(1, (interval.coerceAtLeast(1) * ease * if (grade == 1) 0.75 else if (grade == 3) 1.25 else 1.0).roundToInt())
            }
        }

        val delayMs = when {
            grade == 0 -> 10L * 60L * 1000L
            grade == 1 && interval == 0 -> 8L * 60L * 60L * 1000L
            else -> interval.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        }
        list[index] = old.copy(
            dueAt = System.currentTimeMillis() + delayMs,
            repetitions = reps,
            intervalDays = interval,
            ease = ease,
            lapses = lapses
        )
        persist(list)
    }

    private fun persist(items: List<Phrase>) {
        val arr = JSONArray()
        items.forEach { x ->
            arr.put(JSONObject().apply {
                put("id", x.id); put("ko", x.ko); put("en", x.en)
                put("createdAt", x.createdAt); put("dueAt", x.dueAt)
                put("repetitions", x.repetitions); put("intervalDays", x.intervalDays)
                put("ease", x.ease); put("lapses", x.lapses)
            })
        }
        p.edit().putString("items", arr.toString()).apply()
    }

    companion object {
        private fun legacyInterval(box: Int): Int = intArrayOf(0, 1, 3, 7, 14, 30, 60).getOrElse(box) { 0 }
    }
}
