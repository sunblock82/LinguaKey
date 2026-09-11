package com.linguakey.keyboard

object LearningAnalyzer {
    data class Analysis(
        val literal: String,
        val natural: String,
        /** A sentence length is not a validated CEFR assessment. Retained for saved-format compatibility. */
        val cefr: String,
        val phrases: List<String>,
        val tip: String?,
        val difficultyLabel: String = "CEFR 미평가"
    )

    private val phrasalVerbs = listOf(
        "figure out", "find out", "work out", "pick up", "drop off", "hang out", "catch up",
        "look forward to", "feel like", "end up", "turn out", "come up with", "get along",
        "get back", "go over", "put off", "run into", "take care of", "make sure", "deal with",
        "be supposed to", "be used to", "get used to", "give up", "keep up", "set up", "show up"
    )

    private val phrasePatterns = phrasalVerbs.map { phrase ->
        phrase to Regex("(?<![A-Za-z])" + phrase.split(' ').joinToString("\\s+") { Regex.escape(it) } + "(?![A-Za-z])", RegexOption.IGNORE_CASE)
    }

    fun analyze(korean: String, literalEnglish: String): Analysis {
        val natural = literalEnglish // Never replace a full translation with a phrase-level example.
        val phrases = phrasePatterns.filter { (_, pattern) -> pattern.containsMatchIn(literalEnglish) }.map { it.first }.take(4)
        val curatedTip = ExpressionCoach.tipFor(korean)?.let { "관련 표현 예시 · ${it.title}\n${it.body}" }
        val wordCount = Regex("[A-Za-z]+(?:['’][A-Za-z]+)*").findAll(literalEnglish).count()
        return Analysis(literalEnglish, natural, "미평가", phrases, curatedTip, "영어 ${wordCount}단어 · CEFR 미평가")
    }
}
