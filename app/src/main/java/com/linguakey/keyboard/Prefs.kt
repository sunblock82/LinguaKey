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
}
