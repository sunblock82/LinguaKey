package com.linguakey.keyboard

import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdvancedSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var root: LinearLayout
    private var cloud: CloudTranslator? = null
    private var tab = 0
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); prefs = Prefs(this); title = "LinguaKey 상세설정"; render()
    }
    private fun render() {
        cloud?.cancel()
        if (tab == 1) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(24)) }
        val scroll = ScrollView(this).apply { addView(root); isFillViewport = true }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, i ->
            val b = i.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom); i
        }
        setContentView(scroll)
        val tabs = LinearLayout(this)
        listOf("키보드 상세", "AI 번역").forEachIndexed { index, name ->
            tabs.addView(Button(this).apply { text = name; isEnabled = tab != index; setOnClickListener { tab = index; render() } }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(tabs)
        if (tab == 0) keyboard() else translation()
    }
    private fun keyboard() {
        heading("크기 · 여백 · 글자")
        note("설정을 바꾼 뒤 아래 입력창을 누르거나 키보드를 닫았다 열면 적용됩니다. 홈 버튼 안전 여백은 항상 별도로 유지됩니다.")
        slider("키보드 높이",80,145,prefs.keyHeightPercent,"%") { prefs.keyHeightPercent=it }
        slider("키 사이 간격",1,8,prefs.keyGapDp,"dp") { prefs.keyGapDp=it }
        slider("좌우 여백",0,24,prefs.sidePaddingDp,"dp") { prefs.sidePaddingDp=it }
        slider("추가 하단 여백",0,48,prefs.bottomPaddingDp,"dp") { prefs.bottomPaddingDp=it }
        slider("키 글자 크기",14,28,prefs.keyFontSp,"sp") { prefs.keyFontSp=it }
        slider("키 라벨 자간",0,20,prefs.letterSpacingPercent,"%") { prefs.letterSpacingPercent=it }
        note("키 사이 간격은 버튼 사이 공간, 자간은 ‘한/영’ 같은 키 라벨의 글자 사이 공간입니다. 카톡·문자 입력창 글자 간격은 해당 앱이 정합니다.")
        slider("키 모서리 둥글기",0,18,prefs.cornerRadiusDp,"dp") { prefs.cornerRadiusDp=it }
        choice("테마",listOf("시스템" to "system","밝게" to "light","어둡게" to "dark"),prefs.themeMode) { prefs.themeMode=it }
        toggle("키 테두리 표시",prefs.keyBorder) { prefs.keyBorder=it }
        toggle("숫자열 항상 표시",prefs.numberRowEnabled) { prefs.numberRowEnabled=it }
        heading("타건 반응 · 진동 · 소리")
        toggle("손가락이 닿는 순간 입력",prefs.pressOnTouchDown) { prefs.pressOnTouchDown=it }
        note("끄면 손가락을 뗄 때 입력합니다. 빠른 입력 또는 오타가 적은 쪽으로 선택하세요.")
        toggle("누른 글자 확대 팝업",prefs.keyPopupEnabled) { prefs.keyPopupEnabled=it }
        toggle("키 진동",prefs.hapticEnabled) { prefs.hapticEnabled=it }
        slider("진동 길이",5,40,prefs.vibrationMs,"ms") { prefs.vibrationMs=it }
        toggle("키 소리",prefs.soundEnabled) { prefs.soundEnabled=it }
        slider("소리 크기",0,100,prefs.soundVolume,"%") { prefs.soundVolume=it }
        heading("커서 · 길게 누르기")
        toggle("스페이스바 밀어서 커서 이동",prefs.cursorSwipeEnabled) { prefs.cursorSwipeEnabled=it }
        slider("커서 이동 민감도",1,10,prefs.cursorSensitivity,"단계") { prefs.cursorSensitivity=it }
        slider("삭제 반복 시작 대기",200,800,prefs.repeatDelayMs,"ms") { prefs.repeatDelayMs=it }
        slider("삭제 반복 간격",30,150,prefs.repeatIntervalMs,"ms") { prefs.repeatIntervalMs=it }
        heading("교정 · 입력 옵션")
        toggle("추천/교정 후보 표시",prefs.showSuggestions) { prefs.showSuggestions=it }
        toggle("한국어 띄어쓰기 자동 보정",prefs.autoSpacingEnabled) { prefs.autoSpacingEnabled=it }
        toggle("영어 오타 자동 교정",prefs.englishAutocorrectEnabled) { prefs.englishAutocorrectEnabled=it }
        toggle("같은 자음 두 번으로 쌍자음",prefs.combineDoubleInitials) { prefs.combineDoubleInitials=it }
        toggle("스페이스 두 번으로 마침표",prefs.doubleSpacePeriod) { prefs.doubleSpacePeriod=it }
        heading("바로 입력해 보기")
        root.addView(EditText(this).apply { hint="여기를 눌러 타건감과 간격을 확인하세요"; minLines=3; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE })
    }
    private fun translation() {
        heading("번역 엔진")
        note("GPT·Gemini는 전체 작성문과 아래 상황·말투를 이용해 번역합니다. 상대방 대화나 다른 앱 화면은 자동으로 읽지 않습니다. API 사용은 제공자 계정의 별도 요금·한도를 따릅니다.")
        choice("제공자",listOf("기기 내 기본 번역" to "offline","OpenAI GPT" to "openai","Google Gemini" to "gemini"),prefs.translationProvider) { prefs.translationProvider=it }
        toggle("작성문을 선택한 AI 제공자에게 전송 허용",prefs.cloudConsent) { enabled ->
            if (enabled) AlertDialog.Builder(this).setTitle("클라우드 번역 사용")
                .setMessage("입력을 잠시 멈추면 현재 입력창의 작성문 전체와 지정한 상황·말투를 선택한 제공자에게 전송합니다. API 비용이 발생할 수 있습니다. 비밀번호 입력창과 비공개 모드에서는 전송하지 않습니다.")
                .setPositiveButton("허용") { _,_ -> prefs.cloudConsent=true; render() }
                .setNegativeButton("취소") { _,_ -> prefs.cloudConsent=false; render() }.setOnCancelListener { render() }.show()
            else { prefs.cloudConsent=false }
        }
        slider("입력 후 번역 대기",700,3000,prefs.translationDelayMs,"ms") { prefs.translationDelayMs=it }
        note("대기 시간을 늘리면 요청 횟수가 줄어듭니다. 네트워크 상황에 따라 결과가 표시되기까지 추가 시간이 걸립니다.")
        if (prefs.translationProvider != "offline") {
            val provider=prefs.translationProvider
            val keys=ApiKeyStore(this)
            heading(if(provider=="openai") "OpenAI API 연결" else "Gemini API 연결")
            note(if(keys.has(provider)) "API 키 저장됨 · 새 키를 입력하면 교체됩니다." else "API 키를 아래에 직접 입력하세요. 채팅으로 보내지 마세요.")
            val keyField=EditText(this).apply { hint="API 키"; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(); if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill=android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS }
            keyField.imeOptions = keyField.imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            root.addView(keyField)
            button("API 키 안전하게 저장") {
                if(keyField.text.isNullOrBlank()) { toast("API 키를 입력해주세요"); return@button }
                runCatching { keys.save(provider,keyField.text.toString()) }.onSuccess { keyField.text.clear(); toast("저장했습니다") }.onFailure { toast("키 저장 실패. 기기 잠금 설정을 확인해주세요.") }
            }
            button("이 제공자 API 키 삭제") { runCatching { keys.save(provider,"") }.onSuccess { keyField.text.clear(); toast("삭제했습니다") }.onFailure { toast("삭제하지 못했습니다. 다시 시도해주세요.") } }
            field("모델 이름",if(provider=="openai") prefs.openaiModel else prefs.geminiModel) { if(provider=="openai") prefs.openaiModel=it.trim() else prefs.geminiModel=it.trim() }
            note("기본 모델은 GPT-4.1 mini / Gemini 2.5 Flash입니다. 계정에서 제공되는 모델 이름으로 변경할 수 있습니다.")
        }
        heading("문맥 · 말투")
        field("상황 (예: 친한 연상의 여성에게 보내는 다정한 메시지)",prefs.translationContext,multiline=true) { prefs.translationContext=it.take(1500) }
        field("말투 (예: 자연스러운 일상 대화 / 정중한 업무)",prefs.translationTone) { prefs.translationTone=it.take(120) }
        note("여기에 저장한 상황이 모든 번역에 적용됩니다. 대화 상대가 바뀌면 수정하거나 비워주세요. 입력문은 요약하지 않고 전체 번역하며 6,000자 초과는 알림으로 표시합니다.")
        heading("연결 테스트")
        val sample=EditText(this).apply { imeOptions=android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING; setText("오케이 누나도 얼른 자"); minLines=2 }
        root.addView(sample)
        val result=TextView(this).apply { textSize=16f; setPadding(0,dp(8),0,dp(12)); setTextIsSelectable(true) }
        button("이 문장으로 AI 번역 테스트") {
            if(prefs.translationProvider=="offline") { result.text="먼저 GPT 또는 Gemini를 선택해주세요."; return@button }
            result.text="번역 요청 중…"
            cloud?.close(); cloud=CloudTranslator(this)
            cloud!!.translate(sample.text.toString(), { result.text=it }, { result.text=it.message ?: "연결 실패" })
        }
        root.addView(result)
        heading("개인정보 · 데이터 관리")
        note("기본 번역 SDK인 ML Kit은 문장을 기기에서 처리하지만 기기·앱 정보와 성능·사용 진단 지표를 Google로 보낼 수 있습니다. 시스템 맞춤법 검사기로 문장을 보내지 않습니다. 발음 듣기는 설치된 오프라인 영어 음성을 사용합니다. 이미 클라우드로 전송한 문장은 취소하거나 이 기기 데이터를 삭제해도 제공자 쪽에서 회수되지 않습니다.")
        button("저장 문장·학습 통계 삭제") {
            AlertDialog.Builder(this).setTitle("문장·통계 삭제")
                .setMessage("기기에 저장한 모든 복습 문장과 학습 통계를 영구 삭제합니다. 키보드 설정과 API 키는 유지됩니다.")
                .setPositiveButton("삭제") { _,_ ->
                    getSharedPreferences("phrases", MODE_PRIVATE).edit().clear().apply()
                    getSharedPreferences("learning_stats", MODE_PRIVATE).edit().clear().apply()
                    toast("삭제했습니다")
                }.setNegativeButton("취소",null).show()
        }
        button("클라우드 연결·API 키·지정 문맥 삭제") {
            AlertDialog.Builder(this).setTitle("클라우드 연결 삭제")
                .setMessage("두 제공자의 API 키와 지정한 상황·말투를 지우고 기본 번역으로 변경합니다.")
                .setPositiveButton("삭제") { _,_ ->
                    cloud?.close(); cloud=null
                    prefs.cloudConsent=false; prefs.translationProvider="offline"
                    prefs.translationContext=""; prefs.translationTone="자연스러운 일상 대화"
                    runCatching { ApiKeyStore(this).clear() }.onFailure { toast("키 삭제에 실패했습니다. 다시 시도해주세요.") }; render()
                }.setNegativeButton("취소",null).show()
        }
    }
    private fun heading(s:String) { root.addView(TextView(this).apply { text=s; textSize=21f; setTypeface(typeface,1); setPadding(0,dp(24),0,dp(10)) }) }
    private fun note(s:String) { root.addView(TextView(this).apply { text=s; textSize=14f; setPadding(0,0,0,dp(12)) }) }
    private fun toggle(label:String, checked:Boolean, change:(Boolean)->Unit) { root.addView(Switch(this).apply { text=label; textSize=16f; isChecked=checked; setPadding(0,dp(12),0,dp(12)); setOnCheckedChangeListener { _,v->change(v) } }) }
    private fun slider(label:String,min:Int,max:Int,value:Int,unit:String,change:(Int)->Unit) {
        val text=TextView(this).apply { this.text="$label  $value$unit"; textSize=16f; setPadding(0,dp(12),0,0) }; root.addView(text)
        root.addView(SeekBar(this).apply { this.max=max-min; progress=value-min; setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar:SeekBar?,v:Int,user:Boolean) { text.text="$label  ${v+min}$unit"; if(user) change(v+min) }
            override fun onStartTrackingTouch(bar:SeekBar?)=Unit
            override fun onStopTrackingTouch(bar:SeekBar?)=Unit
        }) })
    }
    private fun choice(label:String,options:List<Pair<String,String>>,current:String,change:(String)->Unit) {
        val index=options.indexOfFirst { it.second==current }.coerceAtLeast(0)
        button("$label: ${options[index].first}") { AlertDialog.Builder(this).setTitle(label).setSingleChoiceItems(options.map{it.first}.toTypedArray(),index) { d,i-> d.dismiss(); change(options[i].second); render() }.setNegativeButton("취소",null).show() }
    }
    private fun field(label:String,value:String,multiline:Boolean=false,save:(String)->Unit) {
        note(label)
        val edit=EditText(this).apply { imeOptions=android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING; setText(value); if(multiline) { minLines=2; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE } else setSingleLine() }
        root.addView(edit); button("저장") { save(edit.text.toString()); toast("저장했습니다") }
    }
    private fun button(label:String,action:()->Unit) { root.addView(Button(this).apply { text=label; isAllCaps=false; setOnClickListener { action() } },LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)) }
    private fun toast(s:String) = Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onStop() { cloud?.cancel(); super.onStop() }
    override fun onDestroy() { cloud?.close(); super.onDestroy() }
}
