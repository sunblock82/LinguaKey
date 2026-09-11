package com.linguakey.keyboard

import android.speech.tts.TextToSpeech
import java.util.Locale

/** Never select a voice advertised by the installed engine as requiring a network. */
internal object OfflineSpeech {
    fun configure(tts: TextToSpeech): Boolean = runCatching {
        val voice = tts.voices.orEmpty()
            .filter { it.locale.language == Locale.ENGLISH.language && !it.isNetworkConnectionRequired }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale.country == "US" }.thenByDescending { it.quality })
            .firstOrNull() ?: return@runCatching false
        tts.setVoice(voice) == TextToSpeech.SUCCESS
    }.getOrDefault(false)

    fun speak(tts: TextToSpeech?, text: String, id: String): Boolean {
        if (tts == null || text.isBlank()) return false
        return runCatching {
            if (!configure(tts)) return false
            val voice = tts.voice ?: return false
            if (voice.isNetworkConnectionRequired) return false
            val parts = TranslationText.chunks(text, TextToSpeech.getMaxSpeechInputLength() - 1)
            parts.forEachIndexed { index, part ->
                if (tts.speak(part, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "$id-$index") != TextToSpeech.SUCCESS) {
                    tts.stop(); return false
                }
            }
            true
        }.getOrDefault(false)
    }
}
