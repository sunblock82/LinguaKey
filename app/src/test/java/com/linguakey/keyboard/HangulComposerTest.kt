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
}
