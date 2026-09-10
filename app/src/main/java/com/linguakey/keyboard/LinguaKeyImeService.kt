package com.linguakey.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

class LinguaKeyImeService : InputMethodService(), TextToSpeech.OnInitListener {
    private lateinit var prefs: Prefs
    private lateinit var translator: TranslatorManager
    private lateinit var pipeline: TranslationPipeline
    private lateinit var store: PhraseStore
    private lateinit var stats: LearningStats
    private lateinit var spellChecker: SystemSpellChecker
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
        store = PhraseStore(this)
        stats = LearningStats(this)
        spellChecker = SystemSpellChecker(this)
        tts = TextToSpeech(this, this)
        resolvePalette()
        if (prefs.translationEnabled) {
            translator.ensureModel(
                onReady = { statusText?.text = "오프라인 번역 준비됨" },
                onError = { statusText?.text = if (prefs.wifiOnlyDownload) "번역 모델 대기 · Wi‑Fi 필요" else "번역 모델 다운로드 실패" }
            )
        }
    }

    private fun resolvePalette() {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (dark) {
            bg = Color.rgb(26, 28, 31); keyBg = Color.rgb(48, 51, 56); keyText = Color.rgb(239, 241, 244)
            secondaryText = Color.rgb(177, 183, 192); accent = Color.rgb(125, 166, 255); panelBg = Color.rgb(35, 38, 42)
        }
    }

    override fun onCreateInputView(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(7))
            setBackgroundColor(bg)
        }
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
            textSize = 15.5f; setTextColor(keyText); maxLines = 2; setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, dp(4), 0)
            setOnClickListener { prefs.learningBarExpanded = !prefs.learningBarExpanded; renderAnalysisDetails() }
        }
        top.addView(naturalText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
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
        if (prefs.numberRowEnabled) addRow(root, (1..9).map { it.toString() } + "0", dp(40))
        val normal = arrayOf(
            listOf("ㅂ","ㅈ","ㄷ","ㄱ","ㅅ","ㅛ","ㅕ","ㅑ","ㅐ","ㅔ"),
            listOf("ㅁ","ㄴ","ㅇ","ㄹ","ㅎ","ㅗ","ㅓ","ㅏ","ㅣ"),
            listOf("SHIFT","ㅋ","ㅌ","ㅊ","ㅍ","ㅠ","ㅜ","ㅡ","⌫")
        )
        val shifted = arrayOf(
            listOf("ㅃ","ㅉ","ㄸ","ㄲ","ㅆ","ㅛ","ㅕ","ㅑ","ㅒ","ㅖ"), normal[1], normal[2]
        )
        (if (shiftState != ShiftState.OFF) shifted else normal).forEach { addRow(root, it, dp(49)) }
        addBottomRow(root)
    }

    private fun buildEnglishKeys(root: LinearLayout) {
        if (prefs.numberRowEnabled) addRow(root, (1..9).map { it.toString() } + "0", dp(40))
        val upper = shiftState != ShiftState.OFF
        val rows = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        addRow(root, rows[0].map { if (upper) it.uppercase() else it.toString() }, dp(49))
        val middle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        middle.setPadding(dp(16), 0, dp(16), 0)
        rows[1].forEach { c -> middle.addView(keyView(if (upper) c.uppercase() else c.toString()) { handleCharacter(if (upper) c.uppercaseChar() else c) }, weighted(1f)) }
        root.addView(middle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)))
        val last = listOf("SHIFT") + rows[2].map { if (upper) it.uppercase() else it.toString() } + "⌫"
        addRow(root, last, dp(49))
        addBottomRow(root)
    }

    private fun buildSymbolKeys(root: LinearLayout) {
        listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","₩","_","&","-","+","(",")","/"),
            listOf("=","*","\"","'",":",";","!","?","⌫")
        ).forEach { addRow(root, it, dp(49)) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(keyView("ABC") { commitComposition(); symbolMode = false; rebuildKeys() }, weighted(1.2f))
        row.addView(keyView("😊") { emojiMode = true; symbolMode = false; rebuildKeys() }, weighted(.85f))
        row.addView(spaceKey(), weighted(3.4f))
        row.addView(keyView(".") { insertLiteral(".") }, weighted(.8f))
        row.addView(keyView(actionLabel()) { performEnter() }, weighted(1.1f))
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(49)))
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
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
    }

    private fun addRow(root: LinearLayout, labels: List<String>, height: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        labels.forEach { label ->
            val view = when (label) {
                "⌫" -> backspaceKey()
                "SHIFT" -> shiftKey()
                else -> keyView(label) { handleCharacter(label[0]) }
            }
            row.addView(view, weighted(if (label == "SHIFT" || label == "⌫") 1.15f else 1f))
        }
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height))
    }

    private fun keyView(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = when { label.length == 1 -> 19f; label.length <= 3 -> 14f; else -> 12f }
        gravity = Gravity.CENTER; setTextColor(keyText); background = rounded(keyBg, 8f); isClickable = true
        setOnClickListener { v -> keyFeedback(v); action() }
    }

    private fun toolButton(label: String, action: () -> Unit) = keyView(label, action).apply {
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
        val v = keyView("⌫") { }
        var repeating = false
        val repeat = object : Runnable {
            override fun run() { if (repeating) { backspace(); handler.postDelayed(this, 55L) } }
        }
        v.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { keyFeedback(view); backspace(); repeating = true; handler.postDelayed(repeat, 360L); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { repeating = false; handler.removeCallbacks(repeat); true }
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
                    if (prefs.cursorSwipeEnabled && abs(event.x - lastX) >= dp(18)) {
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
        if (koreanMode && prefs.autoSpacingEnabled) {
            val source = SentenceExtractor.current(currentInputConnection?.getTextBeforeCursor(500, 0))
            SmartCorrectionEngine.korean(source).firstOrNull()?.let { applySentenceReplacement(source, it.replacement) }
        } else if (!koreanMode && prefs.englishAutocorrectEnabled) {
            val word = SentenceExtractor.currentWord(currentInputConnection?.getTextBeforeCursor(80, 0))
            SmartCorrectionEngine.englishWord(word).firstOrNull()?.let { replaceLastWord(word, it.replacement) }
        }
        currentInputConnection?.commitText(" ", 1)
        scheduleAll(260L)
    }

    private fun handleCharacter(ch: Char) {
        if (symbolMode) {
            commitComposition(); currentInputConnection?.commitText(ch.toString(), 1)
        } else if (koreanMode && isHangulJamo(ch)) {
            val result = composer.feed(ch)
            if (result.commit.isNotEmpty()) currentInputConnection?.commitText(result.commit, 1)
            if (result.composing.isNotEmpty()) currentInputConnection?.setComposingText(result.composing, 1)
            else currentInputConnection?.finishComposingText()
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
        prefs.emojiRecents = list.joinToString("|")
        scheduleAll(300L)
    }

    private fun recentEmojis(): List<String> = prefs.emojiRecents.split("|").filter { it.isNotBlank() }.take(24)

    private fun commitComposition() {
        if (!composer.hasComposition()) return
        composer.finish(); currentInputConnection?.finishComposingText()
    }

    private fun backspace() {
        if (composer.hasComposition()) {
            val r = composer.backspace()
            if (r.composing.isNotEmpty()) currentInputConnection?.setComposingText(r.composing, 1)
            else { currentInputConnection?.setComposingText("", 1); currentInputConnection?.finishComposingText() }
        } else currentInputConnection?.deleteSurroundingText(1, 0)
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

    private fun scheduleAll(delay: Long = 430L) {
        handler.removeCallbacks(translateRunnable); handler.postDelayed(translateRunnable, delay)
        handler.removeCallbacks(suggestionsRunnable); handler.postDelayed(suggestionsRunnable, 90L)
    }

    private fun updateTranslationNow() {
        if (sensitive || manualIncognito) { showPrivateMode(); return }
        if (!prefs.translationEnabled) { naturalText?.text = "실시간 영어 표시가 꺼져 있습니다."; return }
        val source = SentenceExtractor.current(currentInputConnection?.getTextBeforeCursor(900, 0))
        if (source.length < 2 || !source.any { it in '가'..'힣' }) { clearLearningBar(); return }
        currentKorean = source
        val myGeneration = ++generation
        naturalText?.text = "영어 표현 만드는 중…"
        pipeline.translate(source,
            onSuccess = { en ->
                if (myGeneration == generation && !sensitive && !manualIncognito) {
                    currentEnglish = en.trim()
                    currentAnalysis = LearningAnalyzer.analyze(source, currentEnglish)
                    currentNatural = currentAnalysis?.natural.orEmpty()
                    naturalText?.text = currentNatural
                    starButton?.text = "☆"
                    statusText?.text = "온디바이스 · ${currentAnalysis?.cefr ?: ""} · 원문은 입력창에 그대로 유지"
                    renderAnalysisDetails()
                    if (prefs.statsEnabled && sessionRecorded.add(source.hashCode())) {
                        stats.recordTranslation(currentAnalysis?.cefr ?: "A1")
                        while (sessionRecorded.size > 80) sessionRecorded.remove(sessionRecorded.first())
                    }
                }
            },
            onError = {
                if (myGeneration == generation) {
                    naturalText?.text = "번역 모델을 준비하지 못했습니다. LinguaKey 앱에서 모델을 준비해주세요."
                    statusText?.text = if (prefs.wifiOnlyDownload) "Wi‑Fi 연결 후 모델 다운로드" else "모델 다운로드 오류"
                }
            })
    }

    private fun renderAnalysisDetails() {
        val a = currentAnalysis
        val expanded = prefs.learningBarExpanded && a != null && !sensitive && !manualIncognito
        literalText?.visibility = if (expanded) View.VISIBLE else View.GONE
        detailText?.visibility = if (expanded && (a?.phrases?.isNotEmpty() == true || a?.tip != null)) View.VISIBLE else View.GONE
        if (!expanded || a == null) return
        literalText?.text = if (a.natural != a.literal) "직역/기본 번역  ${a.literal}" else "기본 번역  ${a.literal}"
        val pieces = mutableListOf<String>()
        if (a.phrases.isNotEmpty()) pieces += "핵심 표현  ${a.phrases.joinToString(" · ")}"
        a.tip?.let { pieces += it }
        detailText?.text = pieces.joinToString("\n")
    }

    private fun updateSuggestions() {
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
            if (local.isNotEmpty()) renderEnglishSuggestions(word, local)
            else spellChecker.suggest(word) { system ->
                handler.post {
                    val still = SentenceExtractor.currentWord(currentInputConnection?.getTextBeforeCursor(80, 0))
                    if (still.equals(word, true)) renderEnglishSuggestions(word, system)
                }
            }
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
        currentInputConnection?.deleteSurroundingText(original.length, 0)
        currentInputConnection?.commitText(replacement, 1)
    }

    private fun replaceLastWord(original: String, replacement: String) {
        if (original.isBlank() || original == replacement) return
        currentInputConnection?.deleteSurroundingText(original.length, 0)
        currentInputConnection?.commitText(replacement, 1)
    }

    private fun clearLearningBar() {
        currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
        naturalText?.text = "한글을 입력하면 영어가 여기에 표시됩니다."
        literalText?.visibility = View.GONE; detailText?.visibility = View.GONE; starButton?.text = "☆"
        statusText?.text = "온디바이스 · 입력 내용 자동 저장 안 함"
    }

    private fun saveCurrent() {
        if (sensitive || manualIncognito || currentKorean.isBlank() || currentNatural.isBlank()) return
        store.save(currentKorean, currentNatural); starButton?.text = "★"; statusText?.text = "복습 목록에 저장됨"
        if (prefs.statsEnabled) stats.recordSave()
    }

    private fun speakCurrent() {
        if (currentNatural.isBlank() || sensitive || manualIncognito) return
        tts?.speak(currentNatural.replace(" / ", ". "), TextToSpeech.QUEUE_FLUSH, null, "linguakey")
        if (prefs.statsEnabled) stats.recordListen()
    }

    private fun toggleIncognito() {
        manualIncognito = !manualIncognito
        if (manualIncognito) { generation++; currentKorean = ""; currentEnglish = ""; currentNatural = ""; showPrivateMode() }
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
        composer.reset(); generation++; sensitive = SensitiveFieldDetector.isSensitive(attribute)
        currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
        shiftState = ShiftState.OFF; symbolMode = false; emojiMode = false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        sensitive = SensitiveFieldDetector.isSensitive(info)
        rebuildKeys()
        if (sensitive || manualIncognito) showPrivateMode() else scheduleAll(100L)
    }

    override fun onFinishInput() {
        super.onFinishInput(); handler.removeCallbacks(translateRunnable); handler.removeCallbacks(suggestionsRunnable)
        composer.reset(); generation++; currentKorean = ""; currentEnglish = ""; currentNatural = ""; currentAnalysis = null
    }

    override fun onEvaluateFullscreenMode() = false

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.US; tts?.setSpeechRate(0.92f) } }

    private fun keyFeedback(view: View) {
        if (prefs.hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (prefs.soundEnabled) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK, .22f)
    }

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply { setColor(color); cornerRadius = dpF(radiusDp) }
    private fun weighted(w: Float) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpF(v: Float) = v * resources.displayMetrics.density

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); spellChecker.close(); translator.close(); tts?.shutdown(); super.onDestroy()
    }
}
