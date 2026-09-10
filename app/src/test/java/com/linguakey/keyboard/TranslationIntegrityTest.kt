package com.linguakey.keyboard

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class TranslationIntegrityTest {
    @Test fun phraseTipDoesNotReplaceWholeMessage() {
        val source = "오랜만이야. 오늘은 바빠서 못 가고 내일 연락 줄게."
        val translated = "It's been a while. I'm busy today, so I can't make it. I'll text you tomorrow."
        assertEquals(translated, LearningAnalyzer.analyze(source, translated).natural)
    }
    @Test fun chunkingPreservesEveryCharacterAndParagraph() {
        val source = ("오케이 누나도 얼른 자 😊.\n".repeat(130)) + "마지막 문장도 번역해야 해."
        val parts = TranslationText.chunks(source, 180)
        assertEquals(source, parts.joinToString(""))
        assertTrue(parts.all { it.length <= 180 && !Character.isHighSurrogate(it.last()) })
        assertTrue(parts.last().endsWith("마지막 문장도 번역해야 해."))
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedInputFailsInsteadOfTruncating() {
        TranslationText.validate("가".repeat(6001))
    }
    @Test fun openAiAllOutputPartsAreRetained() {
        val json=JSONObject("""{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"Okay."},{"type":"output_text","text":"You should get some sleep too."}]}]}""")
        assertEquals("Okay.\nYou should get some sleep too.",CloudResponse.parse("openai",json))
    }
    @Test(expected = IllegalStateException::class) fun incompleteOpenAiOutputIsNotPresentedAsComplete() {
        CloudResponse.parse("openai",JSONObject("""{"status":"incomplete","output":[]}"""))
    }
    @Test fun geminiThoughtsAreNotShownAsTranslation() {
        val json=JSONObject("""{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Internal thought","thought":true},{"text":"Okay, you should get some sleep too."}]}}]}""")
        assertEquals("Okay, you should get some sleep too.",CloudResponse.parse("gemini",json))
    }
    @Test(expected = IllegalStateException::class) fun geminiTokenLimitIsAnError() {
        CloudResponse.parse("gemini",JSONObject("""{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"Okay sister"}]}}]}"""))
    }
}
