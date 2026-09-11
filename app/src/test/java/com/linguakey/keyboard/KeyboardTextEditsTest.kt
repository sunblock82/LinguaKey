package com.linguakey.keyboard

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class KeyboardTextEditsTest {
    @Test fun eachVisibleEmojiIsDeletedAsOneUnit() {
        listOf("😊", "👍🏽", "👨‍👩‍👧‍👦", "👩🏽‍💻", "🏳️‍🌈", "🇰🇷", "1️⃣", "❤️", "🏴\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F").forEach { emoji ->
            assertEquals(emoji, emoji.length, KeyboardTextEdits.previousGraphemeLength("앞 " + emoji))
        }
    }

    @Test fun multipleFlagsDeleteOnlyTheLastFlag() {
        assertEquals("🇺🇸".length, KeyboardTextEdits.previousGraphemeLength("🇰🇷🇺🇸"))
        assertEquals("🇦".length, KeyboardTextEdits.previousGraphemeLength("🇰🇷🇦"))
    }

    @Test fun normalTextAndCombiningMarksKeepTheirPredecessor() {
        assertEquals(0, KeyboardTextEdits.previousGraphemeLength(""))
        assertEquals(1, KeyboardTextEdits.previousGraphemeLength("한글"))
        assertEquals(1, KeyboardTextEdits.previousGraphemeLength("ab"))
        assertEquals(2, KeyboardTextEdits.previousGraphemeLength("caf\u0065\u0301"))
        assertEquals(2, KeyboardTextEdits.previousGraphemeLength("a\r\n"))
        assertEquals(3, KeyboardTextEdits.previousGraphemeLength("a한"))
    }

    @Test fun deletionReplacesSelectionWithoutDeletingItsNeighbor() {
        val editor = Editor(before = "keep", selected = "remove")
        KeyboardTextEdits.deleteBackward(editor.connection)
        assertEquals("keep", editor.before)
        assertEquals("", editor.selected)
        assertEquals(0, editor.deletions)
        assertEquals(0, editor.batchDepth)
    }

    @Test fun trackedSelectionWorksWhenEditorDoesNotExposeSelectedText() {
        val editor = Editor(before = "keep", selected = "remove", hideSelection = true)
        KeyboardTextEdits.deleteBackward(editor.connection, selectionActive = true)
        assertEquals("keep", editor.before)
        assertEquals("", editor.selected)
        assertEquals(0, editor.deletions)
    }

    @Test fun committedEmojiDeletionKeepsTheRestOfTheSentence() {
        val editor = Editor(before = "안녕하세요 👩🏽‍💻")
        KeyboardTextEdits.deleteBackward(editor.connection)
        assertEquals("안녕하세요 ", editor.before)
        assertEquals(1, editor.deletions)
        assertEquals(0, editor.batchDepth)
    }

    private class Editor(var before: String, var selected: String = "", val hideSelection: Boolean = false) {
        var batchDepth = 0
        var deletions = 0
        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "beginBatchEdit" -> { batchDepth++; true }
                "endBatchEdit" -> { batchDepth--; true }
                "getSelectedText" -> if (hideSelection) null else selected
                "getTextBeforeCursor" -> before
                "commitText" -> { selected = args!![0].toString(); true }
                "deleteSurroundingText" -> { before = before.dropLast(args!![0] as Int); deletions++; true }
                else -> error("Unexpected editor call: ${method.name}")
            }
        } as InputConnection
    }
}
