package com.linguakey.keyboard

object LearningAnalyzer {
    data class Analysis(
        val literal: String,
        val natural: String,
        val cefr: String,
        val phrases: List<String>,
        val tip: String?
    )

    private val phrasalVerbs = listOf(
        "figure out", "find out", "work out", "pick up", "drop off", "hang out", "catch up",
        "look forward to", "feel like", "end up", "turn out", "come up with", "get along",
        "get back", "go over", "put off", "run into", "take care of", "make sure", "deal with",
        "be supposed to", "be used to", "get used to", "give up", "keep up", "set up", "show up"
    )

    private val naturalRules = listOf(
        Regex("아무거나\\s*(괜찮|좋)") to "Anything works for me.",
        Regex("(진짜|정말)?.*아무것도.*하기\\s*싫") to "I really don't feel like doing anything.",
        Regex("잘\\s*모르겠") to "I'm not really sure.",
        Regex("상관\\s*없") to "I don't mind. / Either is fine.",
        Regex("오랜만") to "It's been a while.",
        Regex("연락\\s*줘") to "Let me know. / Text me.",
        Regex("확인해\\s*볼게") to "I'll check and get back to you.",
        Regex("생각해\\s*볼게") to "I'll think about it.",
        Regex("어쩔\\s*수\\s*없") to "It can't be helped. / There's not much we can do.",
        Regex("말이\\s*돼") to "Does that make sense? / Are you serious?"
    )

    fun analyze(korean: String, literalEnglish: String): Analysis {
        val natural = naturalRules.firstOrNull { it.first.containsMatchIn(korean) }?.second ?: literalEnglish
        val normalized = literalEnglish.lowercase()
        val phrases = phrasalVerbs.filter { normalized.contains(it) }.take(4)
        val level = estimateCefr(literalEnglish)
        val curatedTip = ExpressionCoach.tipFor(korean)?.let { "${it.title}: ${it.body}" }
        return Analysis(literalEnglish, natural, level, phrases, curatedTip)
    }

    private fun estimateCefr(text: String): String {
        val words = Regex("[A-Za-z']+").findAll(text).map { it.value.lowercase() }.toList()
        if (words.isEmpty()) return "A1"
        val advanced = setOf("nevertheless", "consequently", "apparently", "presumably", "regardless", "whereas", "despite", "otherwise", "eventually")
        val upper = words.count { it.length >= 9 || it in advanced }
        return when {
            words.size >= 22 || upper >= 3 -> "C1"
            words.size >= 14 || upper >= 2 -> "B2"
            words.size >= 9 || upper >= 1 -> "B1"
            words.size >= 5 -> "A2"
            else -> "A1"
        }
    }
}
