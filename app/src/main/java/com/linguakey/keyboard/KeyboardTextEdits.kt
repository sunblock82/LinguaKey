package com.linguakey.keyboard

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import java.text.BreakIterator
import java.util.Locale

/** Editor operations for committed text. Active Hangul still uses CompositionEdits. */
internal object KeyboardTextEdits {
    fun deleteBackward(connection: InputConnection, selectionActive: Boolean = false) {
        connection.beginBatchEdit()
        try {
            // deleteSurroundingText deliberately excludes the selected text.
            if (selectionActive || !connection.getSelectedText(0).isNullOrEmpty()) {
                connection.commitText("", 1)
                return
            }
            val before = connection.getTextBeforeCursor(2048, 0)
            if (before == null) {
                // Some custom editors do not expose surrounding text. Let that editor
                // handle deletion rather than guessing a UTF-16 unit count.
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                return
            }
            val length = previousGraphemeLength(before.toString())
            if (length > 0) connection.deleteSurroundingText(length, 0)
        } finally {
            connection.endBatchEdit()
        }
    }

    /** UTF-16 length for Android's deleteSurroundingText, never half a surrogate pair. */
    internal fun previousGraphemeLength(text: String): Int {
        if (text.isEmpty()) return 0
        val breaks = BreakIterator.getCharacterInstance(Locale.ROOT)
        breaks.setText(text)
        var start = breaks.preceding(text.length).coerceAtLeast(0)

        // Older Android/Java Unicode tables split newer emoji sequences. Supplement
        // platform segmentation for modifiers, ZWJ families, keycaps and flag tags.
        var emojiStart = previousBaseStart(text, text.length)
        val lastBase = text.codePointAt(emojiStart)
        if (isRegionalIndicator(lastBase)) {
            var count = 0
            var cursor = text.length
            while (cursor > 0 && isRegionalIndicator(text.codePointBefore(cursor))) {
                cursor -= Character.charCount(text.codePointBefore(cursor))
                count++
            }
            emojiStart = text.offsetByCodePoints(text.length, if (count % 2 == 0) -2 else -1)
        } else if (isEmojiBase(lastBase)) {
            while (emojiStart > 0 && text.codePointBefore(emojiStart) == 0x200D) {
                val joinerStart = emojiStart - 1
                if (joinerStart == 0) break
                val previous = previousBaseStart(text, joinerStart)
                if (!isEmojiBase(text.codePointAt(previous))) break
                emojiStart = previous
            }
        }
        start = minOf(start, emojiStart)
        // Defensive for editors that return a cursor inside a surrogate pair.
        if (start > 0 && text[start].isLowSurrogate() && text[start - 1].isHighSurrogate()) start--
        return text.length - start
    }

    private fun previousBaseStart(text: String, end: Int): Int {
        var cursor = end
        while (cursor > 0) {
            val cp = text.codePointBefore(cursor)
            cursor -= Character.charCount(cp)
            if (!isExtension(cp)) return cursor
        }
        return 0
    }

    private fun isExtension(cp: Int): Boolean =
        Character.getType(cp) in setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt()
        ) || cp in 0xFE00..0xFE0F || cp in 0xE0100..0xE01EF ||
            cp in 0x1F3FB..0x1F3FF || cp in 0xE0020..0xE007F

    private fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF
    private fun isEmojiBase(cp: Int) = cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27FF ||
        cp in 0x2300..0x23FF || cp == 0x00A9 || cp == 0x00AE || cp == 0x3030 || cp == 0x303D ||
        cp == 0x3297 || cp == 0x3299
}
