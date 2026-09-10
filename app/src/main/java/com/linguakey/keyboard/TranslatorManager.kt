package com.linguakey.keyboard

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorManager(private val context: Context) {
    private val prefs = Prefs(context)
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.KOREAN)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()
    private val translator: Translator = Translation.getClient(options)

    @Volatile var ready = false
        private set

    fun ensureModel(onReady: () -> Unit, onError: (Exception) -> Unit) {
        val builder = DownloadConditions.Builder()
        if (prefs.wifiOnlyDownload) builder.requireWifi()
        translator.downloadModelIfNeeded(builder.build())
            .addOnSuccessListener { ready = true; onReady() }
            .addOnFailureListener { onError(it) }
    }

    fun translate(text: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        if (!ready) {
            ensureModel(
                onReady = { translate(text, onSuccess, onError) },
                onError = onError
            )
            return
        }
        translator.translate(text)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onError)
    }

    fun close() = translator.close()
}
