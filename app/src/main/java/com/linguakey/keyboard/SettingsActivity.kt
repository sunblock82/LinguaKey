package com.linguakey.keyboard

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var stats: LearningStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        stats = LearningStats(this)
        title = "LinguaKey"
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(34))
        }
        root.addView(TextView(this).apply {
            text = "LinguaKey"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "한글은 그대로 보내고, 영어는 입력하는 순간 배웁니다."
            textSize = 16f
            setPadding(0, dp(6), 0, dp(18))
        })

        root.addView(section("시작하기"))
        root.addView(button("1. LinguaKey 키보드 활성화") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        root.addView(button("2. 현재 키보드를 LinguaKey로 변경") {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            @Suppress("DEPRECATION") imm.showInputMethodPicker()
        })
        val modelStatus = TextView(this).apply {
            text = "최초 사용 전에 한국어→영어 온디바이스 번역 모델을 준비합니다."
            textSize = 13f; setPadding(0, 0, 0, dp(8))
        }
        root.addView(modelStatus)
        root.addView(button("오프라인 번역 모델 지금 준비") {
            modelStatus.text = "모델 확인/다운로드 중…"
            val manager = TranslatorManager(this)
            manager.ensureModel(
                onReady = { modelStatus.text = "✓ 한국어→영어 번역 준비 완료"; manager.close() },
                onError = { modelStatus.text = if (prefs.wifiOnlyDownload) "Wi‑Fi 연결을 확인해주세요." else "모델 준비 실패: ${it.localizedMessage ?: "알 수 없는 오류"}"; manager.close() }
            )
        })
        root.addView(button("저장 문장 복습 · SRS") { startActivity(Intent(this, ReviewActivity::class.java)) })

        root.addView(button("상세설정 · 키 간격/감도 · GPT/Gemini") { startActivity(Intent(this, AdvancedSettingsActivity::class.java)) })
        root.addView(section("입력 UX"))
        root.addView(toggle("추천/교정 후보 표시", prefs.showSuggestions) { prefs.showSuggestions = it })
        root.addView(toggle("고신뢰 한국어 띄어쓰기 자동 보정", prefs.autoSpacingEnabled) { prefs.autoSpacingEnabled = it })
        root.addView(toggle("영어 일반 오타 자동 교정", prefs.englishAutocorrectEnabled) { prefs.englishAutocorrectEnabled = it })
        root.addView(toggle("스페이스바 좌우 스와이프로 커서 이동", prefs.cursorSwipeEnabled) { prefs.cursorSwipeEnabled = it })
        root.addView(toggle("숫자열 항상 표시", prefs.numberRowEnabled) { prefs.numberRowEnabled = it })
        root.addView(toggle("키 진동", prefs.hapticEnabled) { prefs.hapticEnabled = it })
        root.addView(toggle("키 소리", prefs.soundEnabled) { prefs.soundEnabled = it })

        root.addView(section("영어 학습"))
        root.addView(toggle("실시간 영어 표시", prefs.translationEnabled) { prefs.translationEnabled = it })
        root.addView(toggle("표현 팁 · 숙어 · 난이도 표시", prefs.showTips) { prefs.showTips = it })
        root.addView(toggle("학습 통계 저장(문장 원문은 저장 안 함)", prefs.statsEnabled) { prefs.statsEnabled = it })
        root.addView(toggle("번역 모델은 Wi‑Fi에서만 다운로드", prefs.wifiOnlyDownload) { prefs.wifiOnlyDownload = it })

        root.addView(section("학습 통계"))
        val s = stats.snapshot()
        root.addView(TextView(this).apply {
            text = "연속 ${s.streak}일  ·  오늘 번역 ${s.todayTranslated}회\n" +
                    "누적 번역 ${s.translated}  ·  듣기 ${s.listened}  ·  저장 ${s.saved}  ·  복습 ${s.reviewed}\n" +
                    "문장 난이도는 참고용이며 공식 CEFR 평가가 아닙니다."
            textSize = 15f; setLineSpacing(0f, 1.25f)
        })

        root.addView(section("개인정보 보호"))
        root.addView(TextView(this).apply {
            text = "• 비밀번호/PIN 및 앱이 개인화 학습 금지를 요청한 입력창에서는 학습·추천·통계를 자동 중지합니다.\n" +
                    "• 기본 번역은 모델 다운로드 후 기기에서 처리합니다. GPT/Gemini 선택 및 허용 시 작성문과 지정 문맥이 해당 제공자에게 전송됩니다.\n" +
                    "• 일반 입력 문장을 자동 보관하지 않습니다. ☆를 누른 문장만 복습용으로 기기에 저장합니다.\n" +
                    "• 통계에는 번역 횟수 같은 집계값만 저장하며 원문을 저장하지 않습니다.\n" +
                    "• 클립보드, 연락처, 위치, 카메라를 읽지 않습니다.\n• ML Kit은 입력문·번역문을 Google로 보내지 않지만 기기·앱 정보와 성능·사용 진단 지표를 전송할 수 있습니다."
            textSize = 15f; setLineSpacing(0f, 1.28f)
        })

        root.addView(section("현재 구현 범위"))
        root.addView(TextView(this).apply {
            text = "두벌식 한글 조합 · 영문 QWERTY · Shift/Caps Lock · 길게 누르는 백스페이스 반복 · 스페이스바 커서 이동 · 숫자/기호 · 이모지/최근 이모지 · 입력 후보 · 띄어쓰기/오타 보정 · 실시간 번역 · 전체 번역 · 표현 예시 · 참고용 읽기 복잡도 · TTS · 저장 · SM-2 계열 복습 · 집계 통계"
            textSize = 14f; setLineSpacing(0f, 1.25f)
        })

        setContentView(ScrollView(this).apply { addView(root); ScreenInsets.apply(this) })
    }

    private fun section(s: String) = TextView(this).apply {
        text = s; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(24), 0, dp(10))
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(8) }
    }

    @Suppress("UseSwitchCompatOrMaterialCode")
    private fun toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) = Switch(this).apply {
        text = label; textSize = 16f; isChecked = checked; gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(5), 0, dp(5)); setOnCheckedChangeListener { _, v -> change(v) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
