package com.linguakey.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun <T:Activity> withActivity(type: Class<T>, action: (T)->Unit) {
        val activity = instrumentation.startActivitySync(Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        instrumentation.waitForIdleSync()
        try { instrumentation.runOnMainSync { action(type.cast(activity)!!) } }
        finally { instrumentation.runOnMainSync { activity.finish() }; instrumentation.waitForIdleSync() }
    }
    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()

    @Test fun settingsAndReviewOpenAndPrivacyTabIsProtected() {
        withActivity(SettingsActivity::class.java) { assertTrue(views(it.window.decorView).filterIsInstance<Button>().any { b -> b.text.contains("상세설정") }) }
        withActivity(ReviewActivity::class.java) { assertTrue(views(it.window.decorView).filterIsInstance<Button>().any { b -> b.text.contains("전체 보기") }) }
        withActivity(AdvancedSettingsActivity::class.java) { activity ->
            val toggles = views(activity.window.decorView).filterIsInstance<Switch>()
            val touch = toggles.first { it.text.toString() == "손가락이 닿는 순간 입력" }
            touch.isChecked = false
            assertFalse(Prefs(activity).pressOnTouchDown)
            touch.isChecked = true
            views(activity.window.decorView).filterIsInstance<Button>().first { it.text.toString() == "AI 번역" }.performClick()
            assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            assertTrue(views(activity.window.decorView).filterIsInstance<Button>().any { it.text.contains("클라우드 연결·API 키") })
        }
    }

    @Test fun keystoreRoundTripCannotSwapProvidersAndCanBeDeleted() {
        val keys = ApiKeyStore(context)
        val raw = context.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE)
        keys.clear()
        try {
            keys.save("openai", "fake-local-test-key")
            assertEquals("fake-local-test-key", keys.get("openai"))
            val encrypted = raw.getString("openai", "")!!
            assertFalse(encrypted.contains("fake-local-test-key"))
            raw.edit().putString("gemini", encrypted).commit()
            assertEquals("", keys.get("gemini"))
            raw.edit().putString("openai", "v1:corrupt:record").commit()
            assertEquals("", keys.get("openai"))
        } finally { keys.clear() }
        assertEquals("", keys.get("openai"))
        assertEquals("", keys.get("gemini"))
    }

    @Test fun realAndroidEditorKeepsComposedTextSelectionAndEmojiIntact() {
        withActivity(AdvancedSettingsActivity::class.java) { activity ->
            val edit = views(activity.window.decorView).filterIsInstance<EditText>().first()
            edit.requestFocus()
            val connection = edit.onCreateInputConnection(EditorInfo())!!
            val composer = HangulComposer()
            "ㅎㅏㄴㄱㅡㄹ".forEach { CompositionEdits.apply(connection, composer.feed(it)) }
            composer.finish(); connection.finishComposingText()
            assertEquals("한글", edit.text.toString())
            connection.commitText(" 👩🏽‍💻", 1)
            KeyboardTextEdits.deleteBackward(connection)
            assertEquals("한글 ", edit.text.toString())
            edit.setSelection(0, 2)
            KeyboardTextEdits.deleteBackward(connection, true)
            assertEquals(" ", edit.text.toString())
        }
    }
}
