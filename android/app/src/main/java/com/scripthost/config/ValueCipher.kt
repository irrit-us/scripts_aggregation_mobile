package com.scripthost.config

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Value Cipher - encrypts/decrypts individual config values for storage.
 *
 * Implementations are pure JVM (no Android dependencies) so they can be
 * unit-tested outside the Android runtime.
 */
interface ValueCipher {
    /** Encrypt [plain] and return an encoded ciphertext string. */
    fun encrypt(plain: String): String

    /** Reverse [encrypt]. */
    fun decrypt(cipher: String): String
}

/**
 * No-op cipher that stores values as-is. Used as the default so [ConfigStore]
 * stays testable on the JVM without crypto setup.
 */
object PlaintextCipher : ValueCipher {
    override fun encrypt(plain: String): String = plain
    override fun decrypt(cipher: String): String = cipher
}

/**
 * AES-256-GCM cipher. Each encryption uses a fresh random 12-byte IV and the
 * output is `Base64(IV || ciphertext || GCM tag)`.
 *
 * [decrypt] throws [javax.crypto.AEADBadTagException] when the input was
 * tampered with and [IllegalArgumentException] when it is not valid Base64;
 * callers that treat undecryptable values as legacy plaintext must catch these.
 */
class AesGcmValueCipher(private val key: SecretKey) : ValueCipher {

    private val secureRandom = SecureRandom()

    override fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun decrypt(cipher: String): String {
        val combined = Base64.getDecoder().decode(cipher)
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipherInstance = Cipher.getInstance(TRANSFORMATION)
        cipherInstance.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipherInstance.doFinal(ciphertext), Charsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
