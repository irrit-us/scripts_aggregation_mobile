package com.scripthost.config

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Keystore Key Provider - supplies the AES key backing [AesGcmValueCipher].
 *
 * The key is generated once and stored in the AndroidKeyStore, where the key
 * material never leaves the hardware-backed keystore.
 */
class KeystoreKeyProvider(private val keyAlias: String = "scripthost_config_key_v2") {

    /**
     * Return the key stored under [keyAlias], generating it on first use.
     *
     * The v2 alias replaces an earlier key that was generated without
     * `setRandomizedEncryptionRequired(false)`, which made the keystore reject
     * caller-provided IVs and silently broke config persistence. Values written
     * under the old key fail decryption and are treated as legacy plaintext by
     * ConfigStore, then re-encrypted under the new key on the next save.
     */
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(keyAlias)) {
            generateKey()
        }

        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // AesGcmValueCipher supplies its own SecureRandom IV per value;
            // the keystore default forbids caller-provided IVs.
            .setRandomizedEncryptionRequired(false)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
