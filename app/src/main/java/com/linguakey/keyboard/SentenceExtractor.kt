package com.linguakey.keyboard

object SentenceExtractor {
    private val hardBoundary = Regex("[\\n。！？]")

    /** Exact editor suffix for replacement suggestions. Never use this bounded fragment for translation. */
    fun current(textBeforeCursor: CharSequence?): String {
        if (textBeforeCursor == null) return ""
        val raw = textBeforeCursor.toString().takeLast(900)
        val newlineIndex = hardBoundary.findAll(raw).lastOrNull()?.range?.last ?: -1
        var candidate = raw.substring(newlineIndex + 1).trimStart()
        if (candidate.length > 480) {
            var start = candidate.length - 480
            if (Character.isLowSurrogate(candidate[start]) && Character.isHighSurrogate(candidate[start - 1])) start++
            candidate = candidate.substring(start)
        }
        // Trailing spaces and doubled spaces must retain their actual editor length.
        return candidate
    }

    fun currentWord(textBeforeCursor: CharSequence?): String {
        val raw = textBeforeCursor?.toString().orEmpty()
        return raw.takeLastWhile { it.isLetter() || it == '\'' }.takeLast(40)
    }
}
