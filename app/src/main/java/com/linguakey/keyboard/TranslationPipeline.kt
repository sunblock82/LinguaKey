package com.linguakey.keyboard

import android.util.LruCache

class TranslationPipeline(private val translator: TranslatorManager) {
    private val cache = LruCache<String, String>(80)

    fun translate(text: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        cache.get(clean)?.let { onSuccess(it); return }
        val chunks = TranslationText.chunks(clean, 350)
        translateChunk(chunks, 0, mutableListOf(), onSuccess = { parts ->
            val result = parts.joinToString(" ").replace(Regex("\\s+"), " ").trim()
            cache.put(clean, result)
            onSuccess(result)
        }, onError = onError)
    }

    private fun translateChunk(
        chunks: List<String>, index: Int, results: MutableList<String>,
        onSuccess: (List<String>) -> Unit, onError: (Exception) -> Unit
    ) {
        if (index >= chunks.size) { onSuccess(results); return }
        translator.translate(chunks[index], onSuccess = {
            results += it.trim()
            translateChunk(chunks, index + 1, results, onSuccess, onError)
        }, onError)
    }

}
