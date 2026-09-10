package com.linguakey.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

object SensitiveFieldDetector {
    fun isSensitive(info: EditorInfo?): Boolean {
        if (info == null) return true
        val inputType = info.inputType
        val klass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (klass == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) return true
        if (klass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return true
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return true

        val privateOptions = info.privateImeOptions.orEmpty().lowercase()
        if (listOf("nopersonalizedlearning", "incognito", "private", "password", "sensitive")
                .any { privateOptions.contains(it) }) return true
        return false
    }
}
