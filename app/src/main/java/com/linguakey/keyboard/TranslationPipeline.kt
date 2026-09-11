package com.linguakey.keyboard

import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong

/** Offline translation. An invalidated request can neither publish nor launch another chunk. */
class TranslationPipeline internal constructor(
    private val translatePart: (String, (String) -> Unit, (Exception) -> Unit) -> Unit
) {
    constructor(translator: TranslatorManager) : this({ source, success, failure ->
        translator.translate(source, success, failure)
    })

    private val serial = AtomicLong()
    private val cache = object : LinkedHashMap<String, String>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 24
    }

    /** Stops callbacks/chunk progression; an already-running ML Kit task cannot be interrupted. */
    fun cancel() { serial.incrementAndGet() }

    /** Call on input-field changes, private mode and service destruction. Nothing is persisted. */
    fun clear() {
        cancel()
        synchronized(cache) { cache.clear() }
    }

    fun translate(text: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val request = serial.incrementAndGet()
        val clean = try { TranslationText.validate(text) } catch (e: Exception) {
            onError(e); return
        }
        if (clean.isBlank()) { onSuccess(""); return }
        synchronized(cache) { cache[clean] }?.let { onSuccess(it); return }
        // Keep longer sentences together; model latency never blocks keyboard input.
        val chunks = TranslationText.segments(clean, 1000)
        translateChunk(chunks, 0, StringBuilder(), request, onSuccess = { result ->
            val active = synchronized(cache) {
                if (request == serial.get()) { cache[clean] = result; true } else false
            }
            if (active && request == serial.get()) {
                onSuccess(result)
            }
        }, onError = onError)
    }

    private fun translateChunk(
        chunks: List<TranslationText.Segment>, index: Int, result: StringBuilder, request: Long,
        onSuccess: (String) -> Unit, onError: (Exception) -> Unit
    ) {
        if (request != serial.get()) return
        if (index >= chunks.size) { onSuccess(result.toString()); return }
        val chunk = chunks[index]
        val fail: (Exception) -> Unit = { error ->
            if (serial.compareAndSet(request, request + 1)) {
                onError(error)
            }
        }
        try {
            translatePart(chunk.source, { translated ->
                if (request == serial.get()) {
                    if (translated.isBlank()) {
                        fail(IllegalStateException("문장 일부의 번역이 비어 있습니다. 완성된 번역으로 표시하지 않았습니다. 다시 번역해주세요."))
                    } else {
                        result.append(chunk.separatorBefore).append(translated.trim())
                        translateChunk(chunks, index + 1, result, request, onSuccess, onError)
                    }
                }
            }, fail)
        } catch (e: Exception) { fail(e) }
    }
}
