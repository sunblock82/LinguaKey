package com.linguakey.keyboard

/**
 * Stateful Korean 2-beolsik Hangul composer.
 * It returns the entire composing syllable/string; the IME sends it via setComposingText().
 */
class HangulComposer {
    private var l: Int = -1
    private var v: Int = -1
    private var t: Int = 0
    private var raw: String = ""

    private val L = listOf('ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ')
    private val V = listOf('ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ')
    private val T = listOf('\u0000','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ','ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ','ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ')

    private val compoundV = mapOf(
        (8 to 0) to 9, (8 to 1) to 10, (8 to 20) to 11,
        (13 to 4) to 14, (13 to 5) to 15, (13 to 20) to 16,
        (18 to 20) to 19
    )
    private val decomposeV = compoundV.entries.associate { it.value to it.key }

    private val compoundT = mapOf(
        (1 to 19) to 3, (4 to 22) to 5, (4 to 27) to 6,
        (8 to 1) to 9, (8 to 16) to 10, (8 to 17) to 11,
        (8 to 19) to 12, (8 to 25) to 13, (8 to 26) to 14,
        (8 to 27) to 15, (17 to 19) to 18
    )
    private val decomposeT = compoundT.entries.associate { it.value to it.key }

    private val tToL = mapOf(
        1 to 0, 2 to 1, 4 to 2, 7 to 3, 8 to 5, 16 to 6, 17 to 7,
        19 to 9, 20 to 10, 21 to 11, 22 to 12, 23 to 14, 24 to 15,
        25 to 16, 26 to 17, 27 to 18
    )
    private val lToT = mapOf(0 to 1, 1 to 2, 2 to 4, 3 to 7, 5 to 8, 6 to 16, 7 to 17,
        9 to 19, 10 to 20, 11 to 21, 12 to 22, 14 to 23, 15 to 24, 16 to 25, 17 to 26, 18 to 27)

    fun hasComposition(): Boolean = l >= 0 || v >= 0 || t > 0 || raw.isNotEmpty()

    fun reset() { l = -1; v = -1; t = 0; raw = "" }

    fun feed(jamo: Char): Result {
        val li = L.indexOf(jamo)
        val vi = V.indexOf(jamo)
        if (li >= 0) return feedConsonant(li, jamo)
        if (vi >= 0) return feedVowel(vi, jamo)
        val committed = display()
        reset()
        return Result(committed + jamo, "", true)
    }

    fun backspace(): Result {
        if (raw.isNotEmpty()) {
            raw = raw.dropLast(1)
            return Result("", display(), false)
        }
        if (t > 0) {
            t = decomposeT[t]?.first ?: 0
            return Result("", display(), false)
        }
        if (v >= 0) {
            v = decomposeV[v]?.first ?: -1
            return Result("", display(), false)
        }
        if (l >= 0) {
            l = -1
            return Result("", "", false)
        }
        return Result("", "", false)
    }

    private fun feedConsonant(li: Int, ch: Char): Result {
        if (raw.isNotEmpty()) {
            val c = raw
            raw = ch.toString()
            return Result(c, display(), false)
        }
        if (l < 0 && v < 0) { l = li; return Result("", display(), false) }
        if (l >= 0 && v < 0) {
            // Double initial consonants where possible, otherwise commit previous jamo.
            val doubled = when (l to li) {
                0 to 0 -> 1; 3 to 3 -> 4; 7 to 7 -> 8; 9 to 9 -> 10; 12 to 12 -> 13; else -> -1
            }
            if (doubled >= 0) { l = doubled; return Result("", display(), false) }
            val c = display(); l = li; return Result(c, display(), false)
        }
        if (l < 0 && v >= 0) {
            val c = display(); l = li; v = -1; return Result(c, display(), false)
        }
        if (t == 0) {
            val ti = lToT[li]
            if (ti != null) { t = ti; return Result("", display(), false) }
            val c = display(); l = li; v = -1; t = 0; return Result(c, display(), false)
        }
        val nextT = lToT[li]
        val compound = if (nextT != null) compoundT[t to nextT] else null
        if (compound != null) { t = compound; return Result("", display(), false) }
        val c = display(); l = li; v = -1; t = 0; return Result(c, display(), false)
    }

    private fun feedVowel(vi: Int, ch: Char): Result {
        if (raw.isNotEmpty()) {
            val c = raw
            raw = ""
            l = 11 // ㅇ
            v = vi
            return Result(c, display(), false)
        }
        if (l < 0 && v < 0) { v = vi; return Result("", display(), false) }
        if (l >= 0 && v < 0) { v = vi; return Result("", display(), false) }
        if (l < 0 && v >= 0) {
            val cv = compoundV[v to vi]
            if (cv != null) { v = cv; return Result("", display(), false) }
            val c = display(); v = vi; return Result(c, display(), false)
        }
        if (t == 0) {
            val cv = compoundV[v to vi]
            if (cv != null) { v = cv; return Result("", display(), false) }
            val c = display(); l = 11; v = vi; return Result(c, display(), false)
        }

        // Final consonant moves to next syllable when a vowel follows.
        val originalL = l
        val originalV = v
        val split = decomposeT[t]
        return if (split != null) {
            val (firstT, secondT) = split
            val first = compose(originalL, originalV, firstT)
            l = tToL[secondT] ?: 11; v = vi; t = 0
            Result(first.toString(), display(), false)
        } else {
            val movedL = tToL[t]
            if (movedL != null) {
                val first = compose(originalL, originalV, 0)
                l = movedL; v = vi; t = 0
                Result(first.toString(), display(), false)
            } else {
                val c = display(); l = 11; v = vi; t = 0
                Result(c, display(), false)
            }
        }
    }

    fun finish(): String {
        val d = display()
        reset()
        return d
    }

    private fun display(): String {
        if (raw.isNotEmpty()) return raw
        if (l >= 0 && v >= 0) return compose(l, v, t).toString()
        if (l >= 0) return L[l].toString()
        if (v >= 0) return V[v].toString()
        return ""
    }

    private fun compose(l: Int, v: Int, t: Int): Char = (0xAC00 + (l * 21 + v) * 28 + t).toChar()

    data class Result(val commit: String, val composing: String, val finished: Boolean)
}
