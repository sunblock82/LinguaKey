package com.linguakey.keyboard

import android.content.Context

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("linguakey", Context.MODE_PRIVATE)

    var translationEnabled: Boolean
        get() = p.getBoolean("translation_enabled", true)
        set(v) = p.edit().putBoolean("translation_enabled", v).apply()
    var wifiOnlyDownload: Boolean
        get() = p.getBoolean("wifi_only", true)
        set(v) = p.edit().putBoolean("wifi_only", v).apply()
    var hapticEnabled: Boolean
        get() = p.getBoolean("haptic", true)
        set(v) = p.edit().putBoolean("haptic", v).apply()
    var soundEnabled: Boolean
        get() = p.getBoolean("sound", false)
        set(v) = p.edit().putBoolean("sound", v).apply()
    var showTips: Boolean
        get() = p.getBoolean("show_tips", true)
        set(v) = p.edit().putBoolean("show_tips", v).apply()
    var showSuggestions: Boolean
        get() = p.getBoolean("show_suggestions", true)
        set(v) = p.edit().putBoolean("show_suggestions", v).apply()
    var autoSpacingEnabled: Boolean
        get() = p.getBoolean("auto_spacing", true)
        set(v) = p.edit().putBoolean("auto_spacing", v).apply()
    var englishAutocorrectEnabled: Boolean
        get() = p.getBoolean("en_autocorrect", true)
        set(v) = p.edit().putBoolean("en_autocorrect", v).apply()
    var cursorSwipeEnabled: Boolean
        get() = p.getBoolean("cursor_swipe", true)
        set(v) = p.edit().putBoolean("cursor_swipe", v).apply()
    var numberRowEnabled: Boolean
        get() = p.getBoolean("number_row", false)
        set(v) = p.edit().putBoolean("number_row", v).apply()
    var statsEnabled: Boolean
        get() = p.getBoolean("stats_enabled", true)
        set(v) = p.edit().putBoolean("stats_enabled", v).apply()
    var keyPopupEnabled: Boolean
        get() = p.getBoolean("key_popup", true)
        set(v) = p.edit().putBoolean("key_popup", v).apply()
    var learningBarExpanded: Boolean
        get() = p.getBoolean("learning_expanded", false)
        set(v) = p.edit().putBoolean("learning_expanded", v).apply()
    var emojiRecents: String
        get() = p.getString("emoji_recents", "") ?: ""
        set(v) = p.edit().putString("emoji_recents", v).apply()
    var keyHeightPercent: Int
        get() = p.getInt("keyHeightPercent", 110).coerceIn(80, 145)
        set(v) = p.edit().putInt("keyHeightPercent", v.coerceIn(80, 145)).apply()
    var keyGapDp: Int
        get() = p.getInt("keyGapDp", 3).coerceIn(1, 8)
        set(v) = p.edit().putInt("keyGapDp", v.coerceIn(1, 8)).apply()
    var sidePaddingDp: Int
        get() = p.getInt("sidePaddingDp", 6).coerceIn(0, 24)
        set(v) = p.edit().putInt("sidePaddingDp", v.coerceIn(0, 24)).apply()
    var bottomPaddingDp: Int
        get() = p.getInt("bottomPaddingDp", 8).coerceIn(0, 48)
        set(v) = p.edit().putInt("bottomPaddingDp", v.coerceIn(0, 48)).apply()
    var keyFontSp: Int
        get() = p.getInt("keyFontSp", 19).coerceIn(14, 28)
        set(v) = p.edit().putInt("keyFontSp", v.coerceIn(14, 28)).apply()
    var letterSpacingPercent: Int
        get() = p.getInt("letterSpacingPercent", 0).coerceIn(0, 20)
        set(v) = p.edit().putInt("letterSpacingPercent", v.coerceIn(0, 20)).apply()
    var vibrationMs: Int
        get() = p.getInt("vibrationMs", 12).coerceIn(5, 40)
        set(v) = p.edit().putInt("vibrationMs", v.coerceIn(5, 40)).apply()
    var soundVolume: Int
        get() = p.getInt("soundVolume", 20).coerceIn(0, 100)
        set(v) = p.edit().putInt("soundVolume", v.coerceIn(0, 100)).apply()
    var cursorSensitivity: Int
        get() = p.getInt("cursorSensitivity", 5).coerceIn(1, 10)
        set(v) = p.edit().putInt("cursorSensitivity", v.coerceIn(1, 10)).apply()
    var repeatDelayMs: Int
        get() = p.getInt("repeatDelayMs", 350).coerceIn(200, 800)
        set(v) = p.edit().putInt("repeatDelayMs", v.coerceIn(200, 800)).apply()
    var repeatIntervalMs: Int
        get() = p.getInt("repeatIntervalMs", 60).coerceIn(30, 150)
        set(v) = p.edit().putInt("repeatIntervalMs", v.coerceIn(30, 150)).apply()
    var cornerRadiusDp: Int
        get() = p.getInt("cornerRadiusDp", 8).coerceIn(0, 18)
        set(v) = p.edit().putInt("cornerRadiusDp", v.coerceIn(0, 18)).apply()
    var translationDelayMs: Int
        get() = p.getInt("translationDelayMs", 1200).coerceIn(700, 3000)
        set(v) = p.edit().putInt("translationDelayMs", v.coerceIn(700, 3000)).apply()
    var pressOnTouchDown: Boolean
        get() = p.getBoolean("pressOnTouchDown", true)
        set(v) = p.edit().putBoolean("pressOnTouchDown", v).apply()
    var keyBorder: Boolean
        get() = p.getBoolean("keyBorder", false)
        set(v) = p.edit().putBoolean("keyBorder", v).apply()
    var doubleSpacePeriod: Boolean
        get() = p.getBoolean("doubleSpacePeriod", false)
        set(v) = p.edit().putBoolean("doubleSpacePeriod", v).apply()
    var cloudConsent: Boolean
        get() = p.getBoolean("cloudConsent", false)
        set(v) = p.edit().putBoolean("cloudConsent", v).apply()
    var translationProvider: String
        get() = p.getString("translationProvider", "offline") ?: "offline"
        set(v) = p.edit().putString("translationProvider", v).apply()
    var openaiModel: String
        get() = p.getString("openaiModel", "gpt-4.1-mini") ?: "gpt-4.1-mini"
        set(v) = p.edit().putString("openaiModel", v).apply()
    var geminiModel: String
        get() = p.getString("geminiModel", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        set(v) = p.edit().putString("geminiModel", v).apply()
    var translationContext: String
        get() = p.getString("translationContext", "") ?: ""
        set(v) = p.edit().putString("translationContext", v).apply()
    var translationTone: String
        get() = p.getString("translationTone", "자연스러운 일상 대화") ?: "자연스러운 일상 대화"
        set(v) = p.edit().putString("translationTone", v).apply()
    var themeMode: String
        get() = p.getString("themeMode", "system") ?: "system"
        set(v) = p.edit().putString("themeMode", v).apply()
    var combineDoubleInitials: Boolean
        get() = p.getBoolean("combineDoubleInitials", true)
        set(v) = p.edit().putBoolean("combineDoubleInitials", v).apply()
}
