package com.linguakey.keyboard

/** Curated related examples, not a replacement translation or a claim of semantic equivalence. */
object ExpressionCoach {
    data class Tip(val title: String, val body: String)

    private val rules = listOf(
        Regex("아무거나\\s*(괜찮|좋)") to Tip("선택을 맡길 때", "I'm fine with anything. / Anything works for me. 는 어떤 선택이든 괜찮다고 말할 때 쓸 수 있습니다."),
        Regex("하기\\s*싫") to Tip("feel like ~ing", "I feel like going out. 은 ‘나가고 싶은 기분이야’, I don't feel like going out. 은 ‘나가고 싶지 않아’입니다. 예시는 원문 전체의 번역이 아닙니다."),
        Regex("잘\\s*모르겠") to Tip("완곡하게 말하기", "I'm not really sure. 는 I don't know. 보다 부드럽게 들릴 수 있습니다."),
        Regex("상관\\s*없") to Tip("상황별 표현", "선택이라면 Either is fine., 허락이라면 I don't mind. 를 쓸 수 있습니다. ‘관련이 없다’는 뜻이라면 unrelated 같은 다른 표현이 필요합니다."),
        Regex("오랜만") to Tip("인사와 경험 구별", "오랜만에 만난 사람에게 Long time no see! 라고 인사할 수 있습니다. It's been a while since… 는 만남뿐 아니라 오랫동안 하지 않은 일에도 쓸 수 있습니다."),
        Regex("수고(했|하셨)") to Tip("직역 주의", "‘수고했어’는 상황에 따라 Good work! 로 칭찬하거나 Thanks for your hard work. 로 감사할 수 있습니다. 관계와 상황에 맞춰 고르세요."),
        Regex("누나|언니|오빠|형") to Tip("호칭과 관계", "한국어 호칭을 항상 sister/brother로 옮기면 친남매라는 뜻으로 읽힐 수 있습니다. 영어 대화에서는 이름이나 you를 쓰거나 호칭을 생략하기도 합니다. 번역 문맥에 실제 관계를 적으면 도움이 됩니다.")
    )

    fun tipFor(korean: String): Tip? = rules.firstOrNull { it.first.containsMatchIn(korean) }?.second
}
