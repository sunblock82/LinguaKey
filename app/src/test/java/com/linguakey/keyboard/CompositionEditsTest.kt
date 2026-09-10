package com.linguakey.keyboard

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class CompositionEditsTest {
    /** A composing-aware editor that publishes frames only outside a batch. */
    private class Editor {
        var committed = ""
        var composing = ""
        var depth = 0
        val frames = mutableListOf<String>()
        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "beginBatchEdit" -> depth++
                "endBatchEdit" -> { depth--; if (depth == 0) frames += committed + composing }
                "commitText" -> { committed += args!![0].toString(); composing = "" }
                "setComposingText" -> composing = args!![0].toString()
                "finishComposingText" -> { committed += composing; composing = "" }
                else -> error("Unexpected editor call: ${method.name}")
            }
            true
        } as InputConnection
    }

    @Test fun finalConsonantTransferProducesOnlyOneFrame() {
        val e = Editor()
        val c = HangulComposer()
        "ㄱㅏㄴ".forEach { CompositionEdits.apply(e.connection, c.feed(it)) }
        assertEquals("간", e.frames.last())
        val count = e.frames.size
        CompositionEdits.apply(e.connection, c.feed('ㅏ'))
        assertEquals(count + 1, e.frames.size)
        assertEquals("가나", e.frames.last())
        assertEquals(0, e.depth)
    }

    @Test fun continuousSentenceAndBackspaceKeepAllSyllables() {
        val e = Editor()
        val c = HangulComposer()
        "ㅇㅏㄴㄴㅕㅇㅎㅏㅅㅔㅇㅛ".forEach { CompositionEdits.apply(e.connection, c.feed(it)) }
        assertEquals("안녕하세요", e.frames.last())
        CompositionEdits.apply(e.connection, c.backspace())
        assertEquals("안녕하세ㅇ", e.frames.last())
        CompositionEdits.apply(e.connection, c.backspace())
        assertEquals("안녕하세", e.frames.last())
        assertEquals(0, e.depth)
    }

    @Test fun batchClosesEvenWhenTheEditorThrows() {
        var closed = false
        val connection = Proxy.newProxyInstance(
            InputConnection::class.java.classLoader, arrayOf(InputConnection::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "setComposingText" -> throw IllegalStateException("Editor disconnected")
                "endBatchEdit" -> closed = true
            }
            true
        } as InputConnection
        try {
            CompositionEdits.apply(connection, HangulComposer.Result("", "가", false))
            fail("Expected editor failure")
        } catch (_: IllegalStateException) {
            assertTrue(closed)
        }
    }
}
