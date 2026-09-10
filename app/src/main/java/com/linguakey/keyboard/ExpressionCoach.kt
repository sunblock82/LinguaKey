package com.linguakey.keyboard

/** Small, conservative phrase coach. It only emits tips for curated high-confidence patterns. */
object ExpressionCoach {
    data class Tip(val title: String, val body: String)

    private val rules = listOf(
        Regex("아무거나\\s*(괜찮|좋)") to Tip("자연스러운 표현", "I'm fine with anything. / Anything works for me."),
        Regex("하기\\s*싫") to Tip("feel like ~ing", "‘~하고 싶은 기분이다’는 feel like ~ing가 자연스럽습니다. 부정은 don't feel like ~ing."),
        Regex("잘\\s*모르겠") to Tip("완곡하게 말하기", "I'm not really sure. 는 I don't know. 보다 부드럽게 들릴 수 있습니다."),
        Regex("상관\\s*없") to Tip("상황별 표현", "I don't mind. / Either is fine. / It doesn't matter. 는 문맥에 따라 뉘앙스가 다릅니다."),
        Regex("오랜만") to Tip("자주 쓰는 회화", "Long time no see. 보다 It's been a while. 이 더 폭넓게 자연스럽습니다."),
        Regex("수고(했|하셨)") to Tip("직역 주의", "영어에는 ‘수고했어’의 1:1 표현이 없습니다. 상황에 따라 Great work, Thanks for your hard work, Have a good one 등을 씁니다.")
    )

    fun tipFor(korean: String): Tip? = rules.firstOrNull { it.first.containsMatchIn(korean) }?.second
}
