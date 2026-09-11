package com.linguakey.keyboard

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Personal keys stay in app-private encrypted storage. The encryption key never leaves Keystore. */
class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("cloud_credentials", Context.MODE_PRIVATE)

    private fun encryptionKey(create: Boolean): SecretKey? {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        // A restored/corrupt record must never create a replacement key during a read.
        if (!create) return null
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    fun save(provider: String, value: String) = synchronized(LOCK) {
        val normalized = ApiCredentialRules.normalize(provider, value)
        if (normalized.isEmpty()) {
            check(prefs.edit().remove(provider).commit()) { "API 키를 삭제하지 못했습니다. 다시 시도해주세요." }
            return@synchronized
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(create = true))
        cipher.updateAAD(aad(provider))
        val plaintext = normalized.toByteArray(Charsets.UTF_8)
        val encrypted = try { cipher.doFinal(plaintext) } finally { plaintext.fill(0) }
        val envelope = "v1:" + Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString(provider, envelope).commit()) { "API 키를 저장하지 못했습니다. 다시 시도해주세요." }
    }

    fun get(provider: String): String = synchronized(LOCK) {
        ApiCredentialRules.validateProvider(provider)
        val stored = prefs.getString(provider, null) ?: return@synchronized ""
        try {
            require(stored.length <= 12000)
            val parts = stored.split(':')
            val legacy = parts.size == 2
            require(legacy || (parts.size == 3 && parts[0] == "v1"))
            val secret = encryptionKey(create = false) ?: return@synchronized ""
            val iv = Base64.decode(parts[if (legacy) 0 else 1], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[if (legacy) 1 else 2], Base64.NO_WRAP)
            require(iv.size == 12 && encrypted.size >= 16)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv))
            if (!legacy) cipher.updateAAD(aad(provider))
            val plaintext = cipher.doFinal(encrypted)
            val value = try { ApiCredentialRules.normalize(provider, String(plaintext, Charsets.UTF_8)) }
                finally { plaintext.fill(0) }
            // Upgrade earlier encrypted records without ever writing plaintext to preferences.
            if (legacy && value.isNotEmpty()) save(provider, value)
            value
        } catch (_: Exception) {
            // Leave encrypted data intact: a temporarily unavailable Keystore may recover.
            ""
        }
    }

    fun has(provider: String) = get(provider).isNotEmpty()

    /** Erase both providers and the local encryption key. This does not revoke provider-side API keys. */
    fun clear() = synchronized(LOCK) {
        check(prefs.edit().clear().commit()) { "API 키를 삭제하지 못했습니다. 다시 시도해주세요." }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(ALIAS) }
    }

    private fun aad(provider: String) = "LinguaKey:cloud_credentials:v1:$provider".toByteArray(Charsets.UTF_8)

    private companion object {
        const val ALIAS = "linguakey-cloud"
        val LOCK = Any()
    }
}

internal object ApiCredentialRules {
    fun validateProvider(provider: String) {
        require(provider == "openai" || provider == "gemini") { "지원하는 AI 제공자를 선택해주세요." }
    }

    fun normalize(provider: String, value: String): String {
        validateProvider(provider)
        val clean = value.trim()
        require(clean.length <= 4096 && clean.all { it.code in 33..126 }) {
            "API 키에 공백·줄바꿈이 포함되어 있거나 형식이 잘못되었습니다. 키만 입력해주세요."
        }
        return clean
    }
}
