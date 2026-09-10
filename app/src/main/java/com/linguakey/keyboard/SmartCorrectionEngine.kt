package com.linguakey.keyboard

object SmartCorrectionEngine {
    data class Suggestion(val display: String, val replacement: String, val type: Type) {
        enum class Type { SPACING, SPELLING, COMPLETION }
    }

    private val koreanSpacingRules = listOf(
        Regex("할수(있|없)") to { m: MatchResult -> "할 수 ${m.groupValues[1]}" },
        Regex("될수(있|없)") to { m: MatchResult -> "될 수 ${m.groupValues[1]}" },
        Regex("수밖에") to { _: MatchResult -> "수밖에" },
        Regex("것같") to { _: MatchResult -> "것 같" },
        Regex("거같") to { _: MatchResult -> "거 같" },
        Regex("해야되") to { _: MatchResult -> "해야 되" },
        Regex("안되([는면]|$)") to { m: MatchResult -> "안 되${m.groupValues[1]}" },
        Regex("몇시") to { _: MatchResult -> "몇 시" },
        Regex("어떻게해") to { _: MatchResult -> "어떻게 해" },
        Regex("뭐해") to { _: MatchResult -> "뭐 해" }
    )

    private val englishTypos = mapOf(
        "teh" to "the", "dont" to "don't", "doesnt" to "doesn't", "cant" to "can't",
        "wont" to "won't", "im" to "I'm", "ive" to "I've", "id" to "I'd", "ill" to "I'll",
        "becuase" to "because", "definately" to "definitely", "seperate" to "separate",
        "recieve" to "receive", "thier" to "their", "wierd" to "weird", "alot" to "a lot"
    )

    fun korean(sentence: String): List<Suggestion> {
        val out = mutableListOf<Suggestion>()
        koreanSpacingRules.forEach { (regex, replacement) ->
            val m = regex.find(sentence) ?: return@forEach
            val fixed = sentence.replaceRange(m.range, replacement(m))
            if (fixed != sentence) out += Suggestion("띄어쓰기  $fixed", fixed, Suggestion.Type.SPACING)
        }
        return out.distinctBy { it.replacement }.take(3)
    }

    fun englishWord(word: String): List<Suggestion> {
        val clean = word.trim().lowercase()
        val corrected = englishTypos[clean] ?: return emptyList()
        return listOf(Suggestion(corrected, corrected, Suggestion.Type.SPELLING))
    }
}
