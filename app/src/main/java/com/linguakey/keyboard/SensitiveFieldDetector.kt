package com.linguakey.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import java.util.Locale

object SensitiveFieldDetector {
    fun isSensitive(info: EditorInfo?): Boolean {
        if (info == null) return true
        return isSensitive(info.inputType, info.imeOptions, info.privateImeOptions,
            info.hintText?.toString(), info.fieldName)
    }

    internal fun isSensitive(inputType: Int, imeOptions: Int = 0, privateOptions: String? = null,
                             hint: String? = null, fieldName: String? = null): Boolean {
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (klass == InputType.TYPE_NULL || klass == InputType.TYPE_CLASS_PHONE) return true
        if (klass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )) return true
        if (klass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return true
        if (imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return true

        val options = privateOptions.orEmpty().lowercase(Locale.ROOT)
        if (listOf("nopersonalizedlearning", "nolearning", "disablelearning", "incognito", "private", "password", "sensitive")
                .any { options.contains(it) }) return true
        // Apps sometimes mark OTP/account fields as ordinary text. This is a conservative
        // hint-based extra guard; it never examines or stores the typed secret itself.
        val description = listOfNotNull(hint, fieldName).joinToString(" ")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase(Locale.ROOT)
            .replace('_', ' ').replace('-', ' ')
        return sensitiveLabels.any { description.contains(it) } ||
            Regex("\\b(otp|pin|cvv|cvc)\\b").containsMatchIn(description)
    }

    private val sensitiveLabels = listOf("password", "passcode", "verification code", "verificationcode",
        "security code", "one time code", "one time password", "card number", "credit card", "account number",
        "비밀번호", "비밀 번호", "인증번호", "인증 번호", "보안코드", "보안 코드", "주민등록", "계좌번호", "카드번호")
}
