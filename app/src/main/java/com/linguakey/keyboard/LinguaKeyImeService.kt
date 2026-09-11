package com.linguakey.keyboard

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.PopupWindow
import android.view.inputmethod.ExtractedTextRequest
import java.util.Locale
import kotlin.math.abs

class LinguaKeyImeService : InputMethodService(), TextToSpeech.OnInitListener {
    private lateinit var prefs: Prefs
    private lateinit var translator: TranslatorManager
    private lateinit var pipeline: TranslationPipeline
    private lateinit var cloud: CloudTranslator
    private var keyPopup: PopupWindow? = null
    private var lastSpaceTime = 0L
    private var selectionActive = false
    private var translatedGeneration = -1L
    private var repeatDelete: Runnable? = null
    private fun stopRepeatDelete() { repeatDelete?.let { handler.removeCallbacks(it) }; repeatDelete = null }
    private lateinit var store: PhraseStore
    private lateinit var stats: LearningStats
    private val composer = HangulComposer()
    private val handler = Handler(Looper.getMainLooper())

    private var keyboardRoot: LinearLayout? = null
    private var learningBox: LinearLayout? = null
    private var naturalText: TextView? = null
    private var literalText: TextView? = null
    private var detailText: TextView? = null
    private var statusText: TextView? = null
    private var starButton: TextView? = null
    private var candidateRow: LinearLayout? = null
    private var candidateScroll: HorizontalScrollView? = null
    private var currentKorean = ""
    private var currentEnglish = ""
    private var currentNatural = ""
    private var currentAnalysis: LearningAnalyzer.Analysis? = null
    private var sensitive = false
    private var manualIncognito = false
    private var koreanMode = true
    private var symbolMode = false
    private var emojiMode = false
    private var shiftState = ShiftState.OFF
    private var tts: TextToSpeech? = null
    private var generation = 0L
    private var inputActive = false
    private var lastShiftTap = 0L
    private val sessionRecorded = linkedSetOf<Int>()

    private var bg = Color.rgb(239, 242, 247)
    private var keyBg = Color.WHITE
    private var keyText = Color.rgb(18, 24, 33)
    private var secondaryText = Color.rgb(89, 99, 112)
    private var accent = Color.rgb(42, 92, 190)
    private var panelBg = Color.WHITE

    private enum class ShiftState { OFF, ON, LOCKED }
    private val translateRunnable = Runnable { updateTranslationNow() }
    private val suggestionsRunnable = Runnable { updateSuggestions() }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        translator = TranslatorManager(this)
        pipeline = TranslationPipeline(translator)
        cloud = CloudTranslator(this)
        store = PhraseStore(this)
        stats = LearningStats(this)
        tts = TextToSpeech(this, this)
        resolvePalette()
        if (prefs.translationEnabled && prefs.translationProvider == "offline") {
            translator.ensureModel(
                onReady = { statusText?.text = "오프라인 번역 준비됨" },
                onError = { statusText?.text = if (prefs.wifiOnlyDownload) "번역 모델 대기 · Wi‑Fi 필요" else "번역 모델 다운로드 실패" }
            )
        }
    }

    private fun resolvePalette() {
        bg = Color.rgb(239, 242, 247); keyBg = Color.WHITE; keyText = Color.rgb(18, 24, 33)
        secondaryText = Color.rgb(89, 99, 112); accent = Color.rgb(42, 92, 190); panelBg = Color.WHITE
        val dark = prefs.themeMode == "dark" || (prefs.themeMode == "system" && resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        if (dark) {
            bg = Color.rgb(26, 28, 31); keyBg = Color.rgb(48, 51, 56); keyText = Color.rgb(239, 241, 244)
            secondaryText = Color.rgb(177, 183, 192); accent = Color.rgb(125, 166, 255); panelBg = Color.rgb(35, 38, 42)
        }
    }

    override fun onCreateInputView(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(prefs.sidePaddingDp), dp(5), dp(prefs.sidePaddingDp), dp(prefs.bottomPaddingDp))
            setBackgroundColor(bg)
        }
        // Use system-provided navigation insets, not a fixed device-specific gap.
        ViewCompat.setOnApplyWindowInsetsListener(keyboardRoot!!) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(dp(prefs.sidePaddingDp) + bars.left, dp(5), dp(prefs.sidePaddingDp) + bars.right, dp(prefs.bottomPaddingDp) + bars.bottom)
            insets
        }
        keyboardRoot!!.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { ViewCompat.requestApplyInsets(v) }
            override fun onViewDetachedFromWindow(v: View) { stopRepeatDelete(); dismissKeyPopup() }
        })
        keyboardRoot!!.addView(createLearningBar())
        keyboardRoot!!.addView(createCandidateBar())
        rebuildKeys()
        return keyboardRoot!!
    }

    private fun createLearningBar(): View {
        learningBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(8), dp(7))
            background = rounded(panelBg, 12f)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        naturalText = TextView(this).apply {
            text = "한글을 입력하면 자연스러운 영어가 표시됩니다."
            textSize = 15.5f; setTextColor(keyText); minLines = 2; maxLines = Int.MAX_VALUE; setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, dp(4), 0)
            setOnClickListener { prefs.learningBarExpanded = !prefs.learningBarExpanded; renderAnalysisDetails() }
        }
        val translationScroll = ScrollView(this).apply { addView(naturalText); isFillViewport = true }
        top.addView(translationScroll, LinearLayout.LayoutParams(0, dp(96), 1f))
        top.addView(toolButton("🔊") { speakCurrent() })
        starButton = toolButton("☆") { saveCurrent() }; top.addView(starButton)
        top.addView(toolButton("◉") { toggleIncognito() })
        learningBox!!.addView(top)

        literalText = TextView(this).apply {
            textSize = 12.5f; setTextColor(secondaryText); visibility = View.GONE; maxLines = 3; setPadding(0, dp(4), 0, 0)
        }
        detailText = TextView(this).apply {
            textSize = 11.5f; setTextColor(secondaryText); visibility = View.GONE; maxLines = 5; setPadding(0, dp(3), 0, 0)
        }
        statusText = TextView(this).apply {
            text = if (prefs.translationEnabled) "온디바이스 · 한국어 → 영어" else "실시간 영어 표시 꺼짐"
            textSize = 10.5f; setTextColor(secondaryText); setPadding(0, dp(3), 0, 0)
        }
        learningBox!!.addView(literalText)
        learningBox!!.addView(detailText)
        learningBox!!.addView(statusText)
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(toolButton("다시 번역") { currentEnglish = ""; scheduleAll(100L) }, LinearLayout.LayoutParams(0, dp(32), 1f))
        tools.addView(toolButton("상세설정 ⚙") {
            startActivity(Intent(this, AdvancedSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, LinearLayout.LayoutParams(0, dp(32), 1f))
        learningBox!!.addView(tools)
        return learningBox!!
    }

    private fun createCandidateBar(): View {
        candidateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        candidateScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(2), 0, dp(2))
            addView(candidateRow)
        }
        return candidateScroll!!.apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)) }
    }

    private fun rebuildKeys() {
        val root = keyboardRoot ?: return
        stopRepeatDelete(); dismissKeyPopup()
        while (root.childCount > 2) root.removeViewAt(2)
        when {
            emojiMode -> buildEmojiKeys(root)
            symbolMode -> buildSymbolKeys(root)
            koreanMode -> buildKoreanKeys(root)
            else -> buildEnglishKeys(root)
        }
        updateSuggestions()
    }

    private fun buildKoreanKeys(root: LinearLayout) {
        if (prefs.numberRowEnabled) addRow(root, (1..9).map { it.toString() } + "0", rowHeight(40))
        val normal = arrayOf(
            listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ"),
            listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ"),
            listOf("SHIFT","ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫")
        )
        val shifted = arrayOf(
            listOf("ㅃ","ㅉ","ㄸ","ㄲ","ㅆ","ㅛ","ㅕ","ㅑ","ㅒ","ㅖ"), normal[1], normal[2]
        )
        (if (shiftState != ShiftState.OFF) shifted else normal).forEach { addRow(root, it, rowHeight(49)) }
        addBottomRow(root)
    }

    private fun buildEnglishKeys(root: LinearLayout) {
        if (prefs.numberRowEnabled) addRow(root, (1..9).map { it.toString() } + "0", rowHeight(40))
        val upper = shiftState != ShiftState.OFF
        val rows = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        addRow(root, rows[0].map { if (upper) it.uppercase() else it.toString() }, rowHeight(49))
        val middle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        middle.setPadding(dp(16), 0, dp(16), 0)
        rows[1].forEach { c -> middle.addView(keyView(if (upper) c.uppercase() else c.toString(), immediate = true) { handleCharacter(if (upper) c.uppercaseChar() else c) }, weighted(1f)) }
        root.addView(middle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight(49)))
        val last = listOf("SHIFT") + rows[2].map { if (upper) it.uppercase() else it.toString() } + "⌫"
        addRow(root, last, rowHeight(49))
        addBottomRow(root)
    }

    private fun buildSymbolKeys(root: LinearLayout) {
        listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","₩","_","&","-","+","(",")","/"),
            listOf("=","*","\"","'",":",";","!","?","⌫")
        ).forEach { addRow(root, it, rowHeight(49)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(keyView("ABC") { commitComposition(); symbolMode = false; rebuildKeys() }, weighted(1.2f))
        row.addView(keyView("😊") { emojiMode = true; symbolMode = false; rebuildKeys() }, weighted(.85f))
        row.addView(spaceKey(), weighted(3.4f))
        row.addView(keyView(".") { insertLiteral(".") }, weighted(.8f))
        row.addView(keyView(actionLabel()) { performEnter() }, weighted(1.1f))
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight(49)))
    }

    private fun buildEmojiKeys(root: LinearLayout) {
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val recents = recentEmojis()
        if (recents.isNotEmpty()) categories.addView(keyView("🕘") { renderEmojiCategory(root, recents) }, weighted(1f))
        EmojiCatalog.categories.forEach { cat -> categories.addView(keyView(cat.icon) { renderEmojiCategory(root, cat.items) }, weighted(1f)) }
        categories.addView(keyView("ABC") { emojiMode = false; rebuildKeys() }, weighted(1.1f))
        root.addView(categories, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))
        renderEmojiCategory(root, if (recents.isNotEmpty()) recents else EmojiCatalog.categories.first().items)
    }

    private fun renderEmojiCategory(root: LinearLayout, emojis: List<String>) {
        while (root.childCount > 3) root.removeViewAt(3)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        emojis.chunked(8).take(7).forEach { chunk ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEach { emoji -> row.addView(keyView(emoji) { insertEmoji(emoji) }, weighted(1f)) }
            repeat(8 - chunk.size) { row.addView(View(this), weighted(1f)) }
            content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(184)))
    }

    private fun addBottomRow(root: LinearLayout) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(keyView("123") { commitComposition(); symbolMode = true; emojiMode = false; shiftState = ShiftState.OFF; rebuildKeys() }, weighted(.9f))
        row.addView(keyView("😊") { commitComposition(); emojiMode = true; symbolMode = false; rebuildKeys() }, weighted(.85f))
        val lang = keyView(if (koreanMode) "한/영" else "ABC") { commitComposition(); koreanMode = !koreanMode; shiftState = ShiftState.OFF; rebuildKeys() }
        lang.setOnLongClickListener { showImePicker(); true }
        row.addView(lang, weighted(1.0f))
        row.addView(spaceKey(), weighted(3.5f))
        row.addView(keyView(".") { insertLiteral(".") }, weighted(.72f))
        row.addView(keyView(actionLabel()) { performEnter() }, weighted(1.08f))
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight(50)))
    }

    private fun addRow(root: LinearLayout, labels: List<String>, height: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        labels.forEach { label ->
            val view = when (label) {
                "⌫" -> backspaceKey()
                "SHIFT" -> shiftKey()
                else -> keyView(label, immediate = true) { handleCharacter(label[0]) }
            }
            row.addView(view, weighted(if (label == "SHIFT" || label == "⌫") 1.15f else 1f))
        }
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height))
    }

    private fun keyView(label: String, immediate: Boolean = false, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = when { label.length == 1 -> prefs.keyFontSp.toFloat(); label.length <= 3 -> (prefs.keyFontSp - 4).toFloat(); else -> (prefs.keyFontSp - 6).toFloat() }
        letterSpacing = prefs.letterSpacingPercent / 100f
        gravity = Gravity.CENTER; setTextColor(keyText); background = keyBackground(); isClickable = true
        isFocusable = false
        setOnClickListener { v -> keyFeedback(v); action() }
        if (immediate) setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    if (prefs.keyPopupEnabled && !sensitive && !manualIncognito) showKeyPopup(this)
                    if (prefs.pressOnTouchDown) view.performClick()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!prefs.pressOnTouchDown && event.x >= 0 && event.x <= view.width && event.y >= 0 && event.y <= view.height) view.performClick()
                    view.isPressed = false; dismissKeyPopup(); true
                }
                MotionEvent.ACTION_CANCEL -> { view.isPressed = false; dismissKeyPopup(); true }
                else -> true
            }
        }
    }

    private fun toolButton(label: String, action: () -> Unit) = keyView(label, action = action).apply {
        textSize = 14f; minWidth = dp(40); minimumWidth = dp(40); minHeight = dp(36); minimumHeight = dp(36)
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun shiftKey(): TextView {
        val label = when (shiftState) { ShiftState.OFF -> "⇧"; ShiftState.ON -> "⇧"; ShiftState.LOCKED -> "⇧•" }
        return keyView(label) {
            val now = System.currentTimeMillis()
            shiftState = if (shiftState == ShiftState.LOCKED) ShiftState.OFF else if (now - lastShiftTap < 360) ShiftState.LOCKED else if (shiftState == ShiftState.OFF) ShiftState.ON else ShiftState.OFF
            lastShiftTap = now
            rebuildKeys()
        }.apply { if (shiftState != ShiftState.OFF) setTextColor(accent) }
    }

    private fun backspaceKey(): TextView {
        val v = keyView("⌫") { if (inputActive) backspace() }
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopRepeatDelete()
                    if (inputActive) {
                        keyFeedback(view); backspace()
                        val task = object : Runnable {
                            override fun run() {
                                if (inputActive && repeatDelete === this && view.isAttachedToWindow) {
                                    backspace(); handler.postDelayed(this, prefs.repeatIntervalMs.toLong())
                                }
                            }
                        }
                        repeatDelete = task; handler.postDelayed(task, prefs.repeatDelayMs.toLong())
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { stopRepeatDelete(); true }
                else -> true
            }
        }
        return v
    }

    private fun spaceKey(): TextView {
        val v = keyView(if (koreanMode) "한국어" else "English") { }
        var downX = 0f
        var lastX = 0f
        var moved = false
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; lastX = event.x; moved = false; keyFeedback(view); true }
                MotionEvent.ACTION_MOVE -> {
                    if (prefs.cursorSwipeEnabled && abs(event.x - lastX) >= dp(32 - prefs.cursorSensitivity * 2)) {
                        commitComposition(); moved = true
                        val direction = if (event.x > lastX) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, direction))
                        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, direction))
                        lastX = event.x
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved && abs(event.x - downX) < dp(12)) insertSmartSpace(); true }
                MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }
        return v
    }

    private fun insertSmartSpace() {
        commitComposition()
        if (!sensitive && !manualIncognito && koreanMode && prefs.autoSpacingEnabled) {
            val source = SentenceExtractor.current(currentInputConnection?.getTextBeforeCursor(500, 0))
            SmartCorrectionEngine.korean(source).firstOrNull()?.let { applySentenceReplacement(source, it.replacement) }
        } else if (!sensitive && !manualIncognito && !koreanMode && prefs.englishAutocorrectEnabled) {
            val word = SentenceExtractor.currentWord(currentInputConnection?.getTextBeforeCursor(80, 0))
            SmartCorrectionEngine.englishWord(word).firstOrNull()?.let { replaceLastWord(word, it.replacement) }
        }
        val now = android.os.SystemClock.uptimeMillis()
        val before = currentInputConnection?.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (prefs.doubleSpacePeriod && now - lastSpaceTime < 400 && before.endsWith(" ") && before.length == 2 && before[0].isLetterOrDigit()) {
            currentInputConnection?.beginBatchEdit()
            try { currentInputConnection?.deleteSurroundingText(1, 0); currentInputConnection?.commitText(". ", 1) }
            finally { currentInputConnection?.endBatchEdit() }
            lastSpaceTime = 0
        } else { currentInputConnection?.commitText(" ", 1); lastSpaceTime = now }
        scheduleAll(260L)
    }

    private fun handleCharacter(ch: Char) {
        if (!inputActive) return
        lastSpaceTime = 0
        composer.combineDoubleInitials = prefs.combineDoubleInitials
        if (symbolMode) {
            commitComposition(); currentInputConnection?.commitText(ch.toString(), 1)
        } else if (koreanMode && isHangulJamo(ch)) {
            val result = composer.feed(ch)
            currentInputConnection?.let { CompositionEdits.apply(it, result) }
            if (shiftState == ShiftState.ON) { shiftState = ShiftState.OFF; rebuildKeys() }
        } else {
            commitComposition(); currentInputConnection?.commitText(ch.toString(), 1)
            if (shiftState == ShiftState.ON) { shiftState = ShiftState.OFF; rebuildKeys() }
        }
        scheduleAll()
    }

    private fun isHangulJamo(c: Char) = c in 'ㄱ'..'ㅎ' || c in 'ㅏ'..'ㅣ'

    private fun insertLiteral(s: String) {
        commitComposition()
        currentInputConnection?.commitText(s, 1)
        scheduleAll(if (s in listOf(".", "?", "!")) 120L else 320L)
    }

    private fun insertEmoji(emoji: String) {
        commitComposition(); currentInputConnection?.commitText(emoji, 1)
        val list = (listOf(emoji) + recentEmojis().filterNot { it == emoji }).take(24)
        if (!sensitive && !manualIncognito) prefs.emojiRecents = list.joinToString("|")
        scheduleAll(300L)
    }

    private fun recentEmojis(): List<String> = prefs.emojiRecents.split("|").filter { it.isNotBlank() }.take(24)

    private fun commitComposition() {
        if (!composer.hasComposition()) return
        composer.finish(); currentInputConnection?.finishComposingText()
    }

    private fun backspace() {
        if (!inputActive) return
        lastSpaceTime = 0
        if (selectionActive) { composer.reset(); currentInputConnection?.finishComposingText() }
        if (composer.hasComposition()) {
            val r = composer.backspace()
            currentInputConnection?.let { CompositionEdits.apply(it, r) }
        } else currentInputConnection?.let { KeyboardTextEdits.deleteBackward(it, selectionActive) }
        selectionActive = false
        scheduleAll(260L)
    }

    private fun performEnter() {
        commitComposition()
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) currentInputConnection?.performEditorAction(action)
        else {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        scheduleAll(100L)
    }

    private fun actionLabel(): String = when (currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
        EditorInfo.IME_ACTION_GO -> "이동"
        EditorInfo.IME_ACTION_SEARCH -> "검색"
        EditorInfo.IME_ACTION_SEND -> "전송"
        EditorInfo.IME_ACTION_NEXT -> "다음"
        EditorInfo.IME_ACTION_DONE -> "완료"
        else -> "↵"
    }

    private fun scheduleAll(delay: Long = 550L) {
        // Invalidate old asynchronous results immediately when the text changes.
        generation++
        cloud.cancel(); pipeline.cancel()
        handler.removeCallbacks(translateRunnable)
        handler.removeCallbacks(suggestionsRunnable)
        if (!inputActive || sensitive || manualIncognito) return
        if (prefs.translationEnabled) statusText?.text = "입력 중 · 현재 문장 번역 대기"
        handler.postDelayed(translateRunnable, if (prefs.translationProvider == "offline") delay else maxOf(delay, prefs.translationDelayMs.toLong()))
        // Avoid synchronous cross-process cursor reads between rapid keystrokes.
        handler.postDelayed(suggestionsRunnable, 300L)
    }

    private fun updateTranslationNow() {
        if (!inputActive) return
        if (sensitive || manualIncognito) { showPrivateMode(); return }
        if (!prefs.translationEnabled) { naturalText?.text = "실시간 영어 표시가 꺼져 있습니다."; return }
        val source = try {
            val ic = currentInputConnection ?: return
            val extracted = ic.getExtractedText(ExtractedTextRequest().apply { hintMaxChars = TranslationText.MAX_LENGTH + 1; hintMaxLines = 100 }, 0)
            val draft = if (extracted != null && extracted.startOffset == 0 && extracted.partialStartOffset < 0) extracted.text?.toString()
                else null
            TranslationText.validate(draft ?: run {
                val before = ic.getTextBeforeCursor(TranslationText.MAX_LENGTH + 1, 0)
                    ?: error("이 입력창에서 전체 작성문을 읽지 못했습니다.")
                val selected = ic.getSelectedText(0)
                if (selectionActive && selected == null) error("선택한 글자를 읽지 못했습니다. 선택을 해제한 뒤 다시 번역해주세요.")
                val after = ic.getTextAfterCursor(TranslationText.MAX_LENGTH + 1, 0)
                    ?: error("이 입력창에서 전체 작성문을 읽지 못했습니다.")
                before.toString() + selected?.toString().orEmpty() + after
            })
        } catch (e: Exception) {
            clearLearningBar(); naturalText?.text = e.message ?: "입력문을 읽지 못했습니다."; return
        }
        if (source.length < 2 || !source.any { it in '가'..'힣' }) { clearLearningBar(); return }
        if (source == currentKorean && currentEnglish.isNotBlank()) {
            translatedGeneration = generation
            statusText?.text = "현재 문장 번역 완료 · 위아래 스크롤"
            return
        }
        currentKorean = source; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
        literalText?.visibility = View.GONE; detailText?.visibility = View.GONE
        val myGeneration = generation
        val provider = prefs.translationProvider
        val name = when(provider) { "openai" -> "GPT"; "gemini" -> "Gemini"; else -> "기기 내 기본 번역" }
        naturalText?.text = "전체 문장 번역 중…"
        statusText?.text = "$name · 원문 ${source.length}자 · 결과는 위아래로 스크롤"
        val onSuccess: (String) -> Unit = { en ->
            if (myGeneration == generation && inputActive && !sensitive && !manualIncognito) {
                translatedGeneration = myGeneration
                currentEnglish = en.trim()
                currentAnalysis = LearningAnalyzer.analyze(source, currentEnglish)
                currentNatural = currentEnglish
                naturalText?.text = currentEnglish
                starButton?.text = "☆"
                statusText?.text = "$name · 전체 ${source.length}자 번역 · 눌러 학습 팁 / 위아래 스크롤"
                renderAnalysisDetails()
                if (prefs.statsEnabled && sessionRecorded.add(source.hashCode())) {
                    stats.recordTranslation(currentAnalysis?.cefr ?: "A1")
                    while (sessionRecorded.size > 80) sessionRecorded.remove(sessionRecorded.first())
                }
            }
        }
        val onError: (Exception) -> Unit = { e ->
            if (myGeneration == generation && inputActive) {
                currentEnglish = ""; currentNatural = ""
                naturalText?.text = if (provider == "offline") "기본 번역 실패. 앱에서 번역 모델을 준비해주세요." else e.message ?: "클라우드 연결 실패. 다시 시도해주세요."
                statusText?.text = "$name · 번역 미완료"
            }
        }
        if (provider == "offline") pipeline.translate(source, onSuccess, onError)
        else cloud.translate(source, onSuccess, onError)
    }

    private fun renderAnalysisDetails() {
        val a = currentAnalysis
        val expanded = prefs.showTips && prefs.learningBarExpanded && a != null && !sensitive && !manualIncognito
        literalText?.visibility = if (expanded) View.VISIBLE else View.GONE
        detailText?.visibility = if (expanded && (a?.phrases?.isNotEmpty() == true || a?.tip != null)) View.VISIBLE else View.GONE
        if (!expanded || a == null) return
        literalText?.text = a.difficultyLabel + " · 공식 CEFR 평가 아님"
        val pieces = mutableListOf<String>()
        if (a.phrases.isNotEmpty()) pieces += "핵심 표현  ${a.phrases.joinToString(" · ")}"
        a.tip?.let { pieces += it }
        detailText?.text = pieces.joinToString("\n")
    }

    private fun updateSuggestions() {
        if (!inputActive) return
        val row = candidateRow ?: return
        row.removeAllViews()
        if (!prefs.showSuggestions || sensitive || manualIncognito || emojiMode) {
            candidateScroll?.visibility = View.GONE; return
        }
        val before = currentInputConnection?.getTextBeforeCursor(500, 0)
        if (koreanMode) {
            val source = SentenceExtractor.current(before)
            val suggestions = SmartCorrectionEngine.korean(source)
            if (suggestions.isEmpty()) { candidateScroll?.visibility = View.INVISIBLE; return }
            candidateScroll?.visibility = View.VISIBLE
            suggestions.forEach { suggestion ->
                row.addView(candidateChip(suggestion.display) {
                    commitComposition(); applySentenceReplacement(source, suggestion.replacement); scheduleAll(100L)
                })
            }
        } else {
            val word = SentenceExtractor.currentWord(before)
            val local = SmartCorrectionEngine.englishWord(word).map { it.replacement }
            renderEnglishSuggestions(word, local) // No third-party system spellchecker traffic.
        }
    }


    private fun renderEnglishSuggestions(original: String, suggestions: List<String>) {
        val row = candidateRow ?: return
        row.removeAllViews()
        if (suggestions.isEmpty()) { candidateScroll?.visibility = View.INVISIBLE; return }
        candidateScroll?.visibility = View.VISIBLE
        suggestions.distinct().take(3).forEach { candidate ->
            row.addView(candidateChip(candidate) {
                commitComposition(); replaceLastWord(original, candidate); scheduleAll(100L)
            })
        }
    }

    private fun candidateChip(label: String, action: () -> Unit) = TextView(this).apply {
        text = label; textSize = 12.5f; setTextColor(keyText); gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0); background = rounded(panelBg, 14f)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)).apply { marginEnd = dp(6) }
    }

    private fun applySentenceReplacement(original: String, replacement: String) {
        if (original.isBlank() || original == replacement) return
        val connection = currentInputConnection ?: return
        if (sensitive || manualIncognito || selectionActive || !connection.getSelectedText(0).isNullOrEmpty()) return
        if (connection.getTextBeforeCursor(original.length, 0)?.toString() != original) return
        connection.beginBatchEdit()
        try {
            connection.deleteSurroundingText(original.length, 0)
            connection.commitText(replacement, 1)
        } finally { connection.endBatchEdit() }
    }

    private fun replaceLastWord(original: String, replacement: String) {
        if (original.isBlank() || original == replacement) return
        val connection = currentInputConnection ?: return
        if (sensitive || manualIncognito || selectionActive || !connection.getSelectedText(0).isNullOrEmpty()) return
        if (connection.getTextBeforeCursor(original.length, 0)?.toString() != original) return
        connection.beginBatchEdit()
        try {
            connection.deleteSurroundingText(original.length, 0)
            connection.commitText(replacement, 1)
        } finally { connection.endBatchEdit() }
    }

    private fun clearLearningBar() {
        currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
        naturalText?.text = "한글을 입력하면 영어가 여기에 표시됩니다."
        literalText?.visibility = View.GONE; detailText?.visibility = View.GONE; starButton?.text = "☆"
        statusText?.text = "영어 번역 대기 · 입력 내용 자동 저장 안 함"
    }

    private fun saveCurrent() {
        if (sensitive || manualIncognito || translatedGeneration != generation || currentKorean.isBlank() || currentNatural.isBlank()) return
        store.save(currentKorean, currentNatural); starButton?.text = "★"; statusText?.text = "복습 목록에 저장됨"
        if (prefs.statsEnabled) stats.recordSave()
    }

    private fun speakCurrent() {
        if (currentNatural.isBlank() || sensitive || manualIncognito || translatedGeneration != generation) return
        if (OfflineSpeech.speak(tts, currentNatural, "linguakey")) {
            if (prefs.statsEnabled) stats.recordListen()
        } else statusText?.text = "기기에 영어 오프라인 음성을 설치해주세요."
    }

    private fun toggleIncognito() {
        manualIncognito = !manualIncognito
        if (manualIncognito) {
            cloud.cancel(); pipeline.clear(); tts?.stop(); dismissKeyPopup(); generation++
            currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null; sessionRecorded.clear()
            showPrivateMode()
        }
        else { statusText?.text = "학습 모드 다시 켜짐"; scheduleAll(100L) }
        updateSuggestions()
    }

    private fun showPrivateMode() {
        naturalText?.text = "🔒 비공개 입력 · 영어 학습 일시 중지"
        literalText?.visibility = View.GONE; detailText?.visibility = View.GONE
        statusText?.text = if (sensitive) "민감/개인화 금지 입력창 자동 감지" else "수동 비공개 모드"
        starButton?.text = "☆"
    }

    private fun showImePicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        @Suppress("DEPRECATION") imm.showInputMethodPicker()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        handler.removeCallbacks(translateRunnable); handler.removeCallbacks(suggestionsRunnable)
        cloud.cancel(); pipeline.clear(); tts?.stop(); stopRepeatDelete(); dismissKeyPopup(); sessionRecorded.clear()
        selectionActive = attribute?.initialSelStart != attribute?.initialSelEnd
        inputActive = true
        composer.reset(); generation++; sensitive = SensitiveFieldDetector.isSensitive(attribute)
        clearLearningBar()
        currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
        shiftState = ShiftState.OFF; symbolMode = false; emojiMode = false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputActive = true
        sensitive = SensitiveFieldDetector.isSensitive(info)
        resolvePalette()
        setInputView(onCreateInputView())
        keyboardRoot?.let { ViewCompat.requestApplyInsets(it) }
        if (sensitive || manualIncognito) showPrivateMode() else scheduleAll(100L)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        selectionActive = newSelStart != newSelEnd
        if (composer.hasComposition() && (selectionActive || (candidatesEnd >= 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)))) {
            composer.reset(); currentInputConnection?.finishComposingText()
        }
        if (inputActive && (oldSelStart != newSelStart || oldSelEnd != newSelEnd)) scheduleAll()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inputActive = false; generation++; cloud.cancel(); pipeline.clear(); dismissKeyPopup(); stopRepeatDelete(); tts?.stop()
        currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null; sessionRecorded.clear()
        handler.removeCallbacks(translateRunnable); handler.removeCallbacks(suggestionsRunnable)
        clearLearningBar()
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        inputActive = false; cloud.cancel(); pipeline.clear(); dismissKeyPopup(); stopRepeatDelete(); tts?.stop(); sessionRecorded.clear()
        super.onFinishInput(); handler.removeCallbacks(translateRunnable); handler.removeCallbacks(suggestionsRunnable)
        composer.reset(); generation++; currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
    }

    override fun onEvaluateFullscreenMode() = false

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts?.let { OfflineSpeech.configure(it) }; tts?.setSpeechRate(0.92f) } }

    private fun keyFeedback(view: View) {
        if (prefs.hapticEnabled) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(prefs.vibrationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(prefs.vibrationMs.toLong())
            } else view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (prefs.soundEnabled) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, prefs.soundVolume / 100f)
    }

    private fun showKeyPopup(key: TextView) {
        dismissKeyPopup()
        val label = TextView(this).apply {
            text = key.text; textSize = (prefs.keyFontSp + 10).toFloat(); gravity = Gravity.CENTER
            setTextColor(keyText); background = rounded(panelBg, 10f)
        }
        keyPopup = PopupWindow(label, dp(56), dp(66), false).apply {
            isTouchable = false; elevation = dp(5).toFloat()
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            runCatching { showAsDropDown(key, (key.width - dp(56)) / 2, -key.height - dp(66)) }
        }
    }
    private fun dismissKeyPopup() { keyPopup?.dismiss(); keyPopup = null }
    private fun keyBackground(): StateListDrawable {
        val normal = rounded(keyBg, prefs.cornerRadiusDp.toFloat()).apply {
            if (prefs.keyBorder) setStroke(dp(1), secondaryText)
        }
        val pressed = rounded(accent, prefs.cornerRadiusDp.toFloat())
        return StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_pressed), pressed); addState(intArrayOf(), normal) }
    }
    private fun rowHeight(base: Int) = dp(base * prefs.keyHeightPercent / 100)

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply { setColor(color); cornerRadius = dpF(radiusDp) }
    private fun weighted(w: Float) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w).apply { setMargins(dp(prefs.keyGapDp), dp(prefs.keyGapDp), dp(prefs.keyGapDp), dp(prefs.keyGapDp)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpF(v: Float) = v * resources.displayMetrics.density

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); cloud.close(); pipeline.clear(); stopRepeatDelete(); dismissKeyPopup(); translator.close(); tts?.shutdown(); super.onDestroy()
    }
}
