package com.linguakey.keyboard

object TranslationText {
    const val MAX_LENGTH = 6000
    private val paragraphBreak = Regex("\\r\\n|\\r|\\n")

    data class Segment(val source: String, val separatorBefore: String)

    fun validate(text: String): String {
        val clean = text.trim()
        require(clean.length <= MAX_LENGTH) { "전체 입력이 6,000자를 넘습니다. 문장을 나누어 번역해주세요. 생략해서 번역하지 않았습니다." }
        return clean
    }

    /** Parts sent to the offline engine, with original paragraph breaks kept outside the model. */
    fun segments(text: String, max: Int): List<Segment> {
        val result = mutableListOf<Segment>()
        var offset = 0
        var separator = ""

        fun paragraph(raw: String) {
            for (part in chunks(raw.trim(), max)) {
                if (part.isBlank()) continue
                result += Segment(part.trim(), separator)
                separator = " "
            }
        }

        for (boundary in paragraphBreak.findAll(text)) {
            paragraph(text.substring(offset, boundary.range.first))
            // Drop the inter-chunk space, but retain consecutive blank lines.
            separator = separator.trimStart(' ') + boundary.value
            offset = boundary.range.last + 1
        }
        paragraph(text.substring(offset))
        return result
    }

    fun chunks(text: String, max: Int): List<String> {
        require(max >= 2)
        val pieces = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            // Prefer a real sentence ending. A decimal point or URL is not a sentence boundary.
            val sentenceBoundary = (0 until max).lastOrNull { i ->
                val ch = rest[i]
                ch in "。！？\n\r" || (ch in ".?!" && rest.getOrNull(i + 1)?.isWhitespace() == true)
            } ?: -1
            val wordBoundary = (0 until max).lastOrNull { rest[it].isWhitespace() } ?: -1
            val boundary = if (sentenceBoundary >= max / 2) sentenceBoundary else wordBoundary
            var cut = if (boundary >= max / 2) boundary + 1 else max
            if (Character.isHighSurrogate(rest[cut - 1]) && Character.isLowSurrogate(rest[cut])) cut--
            pieces += rest.substring(0, cut)
            rest = rest.substring(cut)
        }
        if (rest.isNotEmpty()) pieces += rest
        return pieces
    }
}
