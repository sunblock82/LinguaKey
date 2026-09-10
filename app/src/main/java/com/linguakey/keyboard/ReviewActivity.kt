package com.linguakey.keyboard

import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReviewActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var store: PhraseStore
    private lateinit var stats: LearningStats
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PhraseStore(this); stats = LearningStats(this); tts = TextToSpeech(this, this)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(30)) }
        root.addView(TextView(this).apply { text = "오늘의 문장 복습"; textSize = 28f; setTypeface(typeface, Typeface.BOLD) })
        val due = store.due()
        val all = store.all()
        root.addView(TextView(this).apply {
            text = "복습 예정 ${due.size}개 · 저장 문장 ${all.size}개\nAgain / Hard / Good / Easy에 따라 다음 복습 간격이 자동 조정됩니다."
            textSize = 14f; setPadding(0, dp(5), 0, dp(16))
        })
        if (due.isEmpty()) {
            root.addView(TextView(this).apply { text = "지금 복습할 문장이 없습니다. 실제 대화에서 배우고 싶은 표현이 나오면 키보드의 ☆를 눌러 저장하세요."; textSize = 17f })
        } else due.take(30).forEach { root.addView(card(it)) }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun card(p: PhraseStore.Phrase) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); setBackgroundColor(0xFFF1F5F9.toInt())
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        addView(TextView(context).apply { text = p.ko; textSize = 16f })
        addView(TextView(context).apply { text = p.en; textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(7), 0, dp(5)) })
        addView(TextView(context).apply { text = "현재 간격 ${p.intervalDays}일 · ease ${"%.2f".format(p.ease)} · lapses ${p.lapses}"; textSize = 12f })
        addView(Button(context).apply { text = "🔊 발음 듣기"; isAllCaps = false; setOnClickListener { speak(p.en) } })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            listOf("다시" to 0, "어려움" to 1, "알겠음" to 2, "쉬움" to 3).forEach { (label, grade) ->
                addView(rateButton(label, grade, p.id), LinearLayout.LayoutParams(0, dp(48), 1f))
            }
        })
    }

    private fun rateButton(label: String, grade: Int, id: String) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setOnClickListener { store.rate(id, grade); stats.recordReview(); render() }
    }

    private fun speak(s: String) { tts?.speak(s, TextToSpeech.QUEUE_FLUSH, null, "review") }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.US; tts?.setSpeechRate(.92f) } }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
