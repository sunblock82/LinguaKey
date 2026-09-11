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
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReviewActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var store: PhraseStore
    private lateinit var stats: LearningStats
    private var tts: TextToSpeech? = null
    private var showAll = false
    private var page = 0

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
        root.addView(Button(this).apply { text = if(showAll) "오늘 복습만 보기" else "저장 문장 전체 보기"; setOnClickListener { showAll=!showAll; page=0; render() } })
        val displayed = if (showAll) all else due
        page = page.coerceIn(0, ((displayed.size - 1) / 20).coerceAtLeast(0))
        if (displayed.isEmpty()) {
            root.addView(TextView(this).apply { text = "지금 복습할 문장이 없습니다. 실제 대화에서 배우고 싶은 표현이 나오면 키보드의 ☆를 눌러 저장하세요."; textSize = 17f })
        } else displayed.drop(page * 20).take(20).forEach { root.addView(card(it)) }
        if (page > 0) root.addView(Button(this).apply { text="이전 20개"; setOnClickListener { page--; render() } })
        if ((page + 1) * 20 < displayed.size) root.addView(Button(this).apply { text="다음 20개"; setOnClickListener { page++; render() } })
        setContentView(ScrollView(this).apply { addView(root); ScreenInsets.apply(this) })
    }

    private fun card(p: PhraseStore.Phrase) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); setBackgroundColor(0xFFF1F5F9.toInt())
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        addView(TextView(context).apply { text = p.ko; textSize = 16f })
        val answer = TextView(context).apply { text = p.en; textSize = 20f; setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(7), 0, dp(5)); visibility = View.GONE; setTextIsSelectable(true) }
        addView(Button(context).apply { text = "영어 답 보기 / 숨기기"; setOnClickListener { answer.visibility = if(answer.visibility == View.VISIBLE) View.GONE else View.VISIBLE } })
        addView(answer)
        addView(TextView(context).apply { text = "다음 복습 간격 ${p.intervalDays}일 · 다시 학습 ${p.lapses}회"; textSize = 12f })
        addView(Button(context).apply { text = "🔊 발음 듣기"; isAllCaps = false; setOnClickListener { speak(p.en) } })
        addView(Button(context).apply { text = "이 문장 삭제"; setOnClickListener {
            AlertDialog.Builder(this@ReviewActivity).setTitle("저장 문장 삭제").setMessage("이 문장을 복습 목록에서 삭제할까요?")
                .setPositiveButton("삭제") { _,_ -> store.delete(p.id); render() }.setNegativeButton("취소",null).show()
        } })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            listOf("다시" to 0, "어려움" to 1, "알겠음" to 2, "쉬움" to 3).forEach { (label, grade) ->
                addView(rateButton(label, grade, p.id), LinearLayout.LayoutParams(0, dp(48), 1f))
            }
        })
    }

    private fun rateButton(label: String, grade: Int, id: String) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setOnClickListener { store.rate(id, grade); if (Prefs(this@ReviewActivity).statsEnabled) stats.recordReview(); render() }
    }

    private fun speak(s: String) { if(!OfflineSpeech.speak(tts, s, "review")) android.widget.Toast.makeText(this,"기기에 영어 오프라인 음성을 설치해주세요.",android.widget.Toast.LENGTH_LONG).show() }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts?.let { OfflineSpeech.configure(it) }; tts?.setSpeechRate(.92f) } }
    override fun onStop() { tts?.stop(); super.onStop() }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
