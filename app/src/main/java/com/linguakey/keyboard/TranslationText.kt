package com.linguakey.keyboard

object TranslationText {
    const val MAX_LENGTH = 6000
    fun validate(text: String): String {
        val clean = text.trim()
        require(clean.length <= MAX_LENGTH) { "전체 입력이 6,000자를 넘습니다. 문장을 나누어 번역해주세요. 생략해서 번역하지 않았습니다." }
        return clean
    }
    fun chunks(text: String, max: Int): List<String> {
        require(max >= 2)
        val pieces = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            val boundary = rest.lastIndexOfAny(charArrayOf('.', '?', '!', '。', '？', '！', '\n', ' '), max - 1)
            var cut = if (boundary >= max / 2) boundary + 1 else max
            if (Character.isHighSurrogate(rest[cut - 1]) && Character.isLowSurrogate(rest[cut])) cut--
            pieces += rest.substring(0, cut)
            rest = rest.substring(cut)
        }
        if (rest.isNotEmpty()) pieces += rest
        return pieces
    }
}
