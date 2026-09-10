package com.linguakey.keyboard

import android.content.Context
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class SystemSpellChecker(context: Context) : SpellCheckerSession.SpellCheckerSessionListener {
    private val manager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    private var session: SpellCheckerSession? = null
    private var pendingWord: String = ""
    private var pendingCallback: ((List<String>) -> Unit)? = null

    init {
        runCatching {
            session = manager.newSpellCheckerSession(null, Locale.US, this, true)
        }
    }

    fun suggest(word: String, callback: (List<String>) -> Unit) {
        if (word.length < 2 || !word.all { it.isLetter() || it == '\'' }) { callback(emptyList()); return }
        val s = session ?: run { callback(emptyList()); return }
        pendingWord = word
        pendingCallback = callback
        @Suppress("DEPRECATION")
        s.getSuggestions(TextInfo(word), 5)
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        val result = results?.firstOrNull() ?: run { pendingCallback?.invoke(emptyList()); return }
        val out = mutableListOf<String>()
        for (i in 0 until result.suggestionsCount) {
            val candidate = result.getSuggestionAt(i)
            if (candidate.isNotBlank() && !candidate.equals(pendingWord, true)) out += candidate
        }
        pendingCallback?.invoke(out.distinct().take(3))
        pendingCallback = null
    }

    override fun onGetSentenceSuggestions(results: Array<out android.view.textservice.SentenceSuggestionsInfo>?) = Unit

    fun close() { session?.close(); session = null }
}
