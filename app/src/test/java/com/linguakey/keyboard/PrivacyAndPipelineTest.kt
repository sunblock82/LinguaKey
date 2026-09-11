package com.linguakey.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Locale

class PrivacyAndPipelineTest {
    @Test fun sensitiveFieldsStopLearningEvenWithOrdinaryTextClass() {
        assertTrue(SensitiveFieldDetector.isSensitive(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertTrue(SensitiveFieldDetector.isSensitive(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        listOf("인증번호", "verificationCode", "OTP", "card_number", "비밀번호").forEach {
            assertTrue(it, SensitiveFieldDetector.isSensitive(InputType.TYPE_CLASS_TEXT, hint=it))
        }
        assertFalse(SensitiveFieldDetector.isSensitive(InputType.TYPE_CLASS_TEXT, hint="메시지 작성"))
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            assertTrue(SensitiveFieldDetector.isSensitive(InputType.TYPE_CLASS_TEXT, privateOptions="INCOGNITO"))
        } finally { Locale.setDefault(old) }
    }

    @Test fun endpointsCannotEmbedAnotherHostOrQuery() {
        assertEquals("https://api.openai.com/v1/responses", CloudRequestPolicy.endpoint("openai", "gpt-4.1-mini"))
        listOf("../other", "m?key=secret", "https://evil.invalid", "m\r\nHeader:value").forEach {
            assertThrows(IllegalArgumentException::class.java) { CloudRequestPolicy.endpoint("gemini", it) }
        }
        assertThrows(IllegalArgumentException::class.java) { ApiCredentialRules.normalize("unknown", "abc") }
        assertThrows(IllegalArgumentException::class.java) { ApiCredentialRules.normalize("openai", "a\r\nb") }
    }

    @Test fun oversizedAndRefusedResponsesNeverAppearAsTranslations() {
        assertThrows(IllegalStateException::class.java) { CloudResponse.readBounded(ByteArrayInputStream(ByteArray(30)), 20) }
        assertThrows(IllegalStateException::class.java) {
            CloudResponse.parse("openai", JSONObject("""{"status":"completed","output":[{"type":"message","content":[{"type":"refusal","refusal":"No"}]}]}"""))
        }
    }

    @Test fun offlinePreservesParagraphsAndEveryClause() {
        val received = mutableListOf<String>()
        val pipeline = TranslationPipeline { text, success, _ -> received += text; success("<$text>") }
        var result = ""
        pipeline.translate("첫 문장.\n\n마지막 문장.", { result=it }, { throw it })
        assertEquals(listOf("첫 문장.", "마지막 문장."), received)
        assertEquals("<첫 문장.>\n\n<마지막 문장.>", result)
    }

    @Test fun privacyClearStopsOldCallbacksAndDoesNotCacheThem() {
        val callbacks = mutableListOf<(String)->Unit>()
        var published = false
        val pipeline = TranslationPipeline { _, success, _ -> callbacks += success }
        pipeline.translate("첫 문장\n두 번째 문장", { published=true }, { throw it })
        pipeline.clear()
        callbacks[0]("old private text")
        assertFalse(published)
        assertEquals(1, callbacks.size)
        pipeline.translate("첫 문장\n두 번째 문장", { published=true }, { throw it })
        assertEquals(2, callbacks.size)
    }

    @Test fun emptyOfflineChunkFailsEntireTranslation() {
        var result: String? = null
        var failed = false
        val pipeline = TranslationPipeline { _, success, _ -> success("") }
        pipeline.translate("번역할 문장", { result=it }, { failed=true })
        assertNull(result)
        assertTrue(failed)
    }
}
