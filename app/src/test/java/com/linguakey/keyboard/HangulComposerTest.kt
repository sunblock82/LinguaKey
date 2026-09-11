package com.linguakey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class HangulComposerTest {
    private fun type(keys: String): String {
        val c = HangulComposer(); val out = StringBuilder()
        keys.forEach { k ->
            val r = c.feed(k)
            out.append(r.commit)
        }
        out.append(c.finish())
        return out.toString()
    }

    @Test fun basic() { assertEquals("한글", type("ㅎㅏㄴㄱㅡㄹ")) }
    @Test fun finalMoves() { assertEquals("가나", type("ㄱㅏㄴㅏ")) }
    @Test fun compoundFinalSplits() { assertEquals("읽어", type("ㅇㅣㄹㄱㅇㅓ")) }
    @Test fun compoundVowel() { assertEquals("과", type("ㄱㅗㅏ")) }
    @Test fun doubleInitial() { assertEquals("까", type("ㄱㄱㅏ")) }

    @Test fun consecutiveVowelsDoNotInventInitialConsonants() {
        assertEquals("가ㅏ", type("ㄱㅏㅏ"))
        assertEquals("과ㅣ", type("ㄱㅗㅏㅣ"))
        assertEquals("아ㅠㅠ", type("ㅇㅏㅠㅠ"))
    }

    @Test fun repeatedInitialCanBeDisabledForChatJamo() {
        val c = HangulComposer().apply { combineDoubleInitials = false }
        val out = StringBuilder()
        "ㄱㄱㅏ".forEach { out.append(c.feed(it).commit) }
        out.append(c.finish())
        assertEquals("ㄱ가", out.toString())
    }

    @Test fun deletingTwoTapDoubleInitialUndoesOnlyOneTap() {
        val c = HangulComposer()
        c.feed('ㄱ'); c.feed('ㄱ')
        assertEquals("ㄱ", c.backspace().composing)
        assertEquals("", c.backspace().composing)
    }

    @Test fun deletingShiftedDoubleInitialRemovesOneWholeKey() {
        val c = HangulComposer()
        c.feed('ㄲ')
        assertEquals("", c.backspace().composing)
    }

    @Test fun doubleInitialHistoryStaysWithItsOwnSyllable() {
        val c = HangulComposer()
        "ㄱㄱㅏㄴㅏ".forEach { c.feed(it) }
        assertEquals("ㄴ", c.backspace().composing)
        assertEquals("", c.backspace().composing)
        assertEquals("ㅏ", c.feed('ㅏ').composing)
    }

    @Test fun compoundFinalReallySplitsWithoutAnExplicitInitial() {
        assertEquals("일거", type("ㅇㅣㄹㄱㅓ"))
        assertEquals("갑시", type("ㄱㅏㅂㅅㅣ"))
    }

    @Test fun clusterDeletionPeelsOffOneJamoAtATime() {
        val c = HangulComposer()
        "ㄱㅗㅏㄴㅈ".forEach { c.feed(it) }
        assertEquals("관", c.backspace().composing)
        assertEquals("과", c.backspace().composing)
        assertEquals("고", c.backspace().composing)
        assertEquals("ㄱ", c.backspace().composing)
        assertEquals("", c.backspace().composing)
    }
}
