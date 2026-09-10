package com.linguakey.keyboard

import org.junit.Assert.*
import org.junit.Test

class LearningCoreTest {
    @Test fun highConfidenceSpacing() {
        val s = SmartCorrectionEngine.korean("오늘 할수있어")
        assertTrue(s.first().replacement.contains("할 수 있어"))
    }

    @Test fun commonEnglishTypo() {
        assertEquals("the", SmartCorrectionEngine.englishWord("teh").first().replacement)
    }

    @Test fun naturalExpression() {
        val a = LearningAnalyzer.analyze("아무거나 괜찮아", "Anything is okay.")
        assertEquals("Anything is okay.", a.natural)
    }

    @Test fun sentenceContext() {
        assertEquals("오늘 뭐해", SentenceExtractor.current("어제는 바빴어。오늘 뭐해"))
    }
}
