package com.linguakey.keyboard

object SentenceExtractor {
    private val hardBoundary = Regex("[\\n。！？]")

    /** Returns recent meaningful Korean context, preserving up to three short sentences for stable translation. */
    fun current(textBeforeCursor: CharSequence?): String {
        if (textBeforeCursor == null) return ""
        val raw = textBeforeCursor.toString().takeLast(900)
        val newlineIndex = hardBoundary.findAll(raw).lastOrNull()?.range?.last ?: -1
        var candidate = raw.substring(newlineIndex + 1).trim()
        if (candidate.length > 480) candidate = candidate.takeLast(480)
        return candidate.replace(Regex("[ \\t]+"), " ")
    }

    fun currentWord(textBeforeCursor: CharSequence?): String {
        val raw = textBeforeCursor?.toString().orEmpty()
        return raw.takeLastWhile { it.isLetter() || it == '\'' }.takeLast(40)
    }
}
