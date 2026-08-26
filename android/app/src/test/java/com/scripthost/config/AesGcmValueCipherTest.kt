package com.scripthost.config

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.KeyGenerator

/**
 * Unit tests for [AesGcmValueCipher] covering round-trips, IV randomness and
 * tamper/wrong-key detection, plus the [PlaintextCipher] identity behavior.
 */
class AesGcmValueCipherTest {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesGcmValueCipher(key)

    @Test
    fun encryptDecrypt_roundTripsPlaintext() {
        val plaintext = "sk-test-secret-value"

        assertThat(cipher.decrypt(cipher.encrypt(plaintext))).isEqualTo(plaintext)
    }

    @Test
    fun encrypt_samePlaintextTwice_producesDifferentCiphertext() {
        val first = cipher.encrypt("same plaintext")
        val second = cipher.encrypt("same plaintext")

        assertThat(first).isNotEqualTo(second)
        assertThat(cipher.decrypt(first)).isEqualTo("same plaintext")
        assertThat(cipher.decrypt(second)).isEqualTo("same plaintext")
    }

    @Test
    fun decrypt_tamperedCiphertext_throws() {
        val ciphertext = cipher.encrypt("some secret")
        val index = ciphertext.length / 2
        val tampered = ciphertext.substring(0, index) +
            (if (ciphertext[index] == 'A') 'B' else 'A') +
            ciphertext.substring(index + 1)

        assertThrows(Exception::class.java) { cipher.decrypt(tampered) }
    }

    @Test
    fun decrypt_withDifferentKey_throws() {
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val otherCipher = AesGcmValueCipher(otherKey)
        val ciphertext = cipher.encrypt("some secret")

        assertThrows(Exception::class.java) { otherCipher.decrypt(ciphertext) }
    }

    @Test
    fun decrypt_malformedCiphertext_throws() {
        assertThrows(Exception::class.java) { cipher.decrypt("not-valid-ciphertext") }
    }

    @Test
    fun plaintextCipher_isIdentity() {
        assertThat(PlaintextCipher.encrypt("plain value")).isEqualTo("plain value")
        assertThat(PlaintextCipher.decrypt("plain value")).isEqualTo("plain value")
    }
}
