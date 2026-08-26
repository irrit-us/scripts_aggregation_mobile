package com.scripthost.security

import com.google.common.truth.Truth.assertThat
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.VerificationResult
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

/**
 * Unit tests for [SignatureVerifier].
 */
class SignatureVerifierTest {

    private lateinit var verifier: SignatureVerifier

    @Before
    fun setUp() {
        verifier = SignatureVerifier()
    }

    private fun testScript(sourceCode: String = "console.log('test');"): Script {
        return Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = listOf(Permission.INTERNET),
            sourceCode = sourceCode
        )
    }

    @Test
    fun generateKeyPair_producesKeys() {
        val keyPair = verifier.generateKeyPair()
        assertThat(keyPair.private).isNotNull()
        assertThat(keyPair.public).isNotNull()
    }

    @Test
    fun signAndVerify_roundTripsWithMatchingKey() {
        val keyPair = verifier.generateKeyPair()
        val signedScript = keyPair.signedScript()

        val result = SignatureVerifier(keyPair.public).verify(signedScript)
        assertThat(result is VerificationResult.Valid).isTrue()
    }

    @Test
    fun verify_rejectsTamperedSourceCode() {
        val keyPair = verifier.generateKeyPair()
        val signature = verifier.sign(testScript(), keyPair.private)
        val tampered = testScript(sourceCode = "console.log('tampered');").copy(signature = signature)

        val result = SignatureVerifier(keyPair.public).verify(tampered)
        assertThat(result is VerificationResult.Invalid).isTrue()
    }

    @Test
    fun verify_rejectsSignatureFromDifferentKey() {
        val signingKeyPair = verifier.generateKeyPair()
        val verifyingKeyPair = verifier.generateKeyPair()

        val signature = verifier.sign(testScript(), signingKeyPair.private)
        val script = testScript().copy(signature = signature)

        val result = SignatureVerifier(verifyingKeyPair.public).verify(script)
        assertThat(result is VerificationResult.Invalid).isTrue()
    }

    @Test
    fun verify_rejectsMissingSignature() {
        val result = verifier.verify(testScript().copy(signature = null))
        assertThat(result is VerificationResult.Invalid).isTrue()
    }

    @Test
    fun computeHash_isStableForSameContent() {
        val hash1 = verifier.computeHash(testScript())
        val hash2 = verifier.computeHash(testScript())
        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun computeHash_changesWithContent() {
        val original = verifier.computeHash(testScript())
        val modified = verifier.computeHash(testScript(sourceCode = "console.log('modified');"))
        assertThat(original).isNotEqualTo(modified)
    }

    @Test
    fun verifyHash_detectsMismatch() {
        val script = testScript()
        assertThat(verifier.verifyHash(script, verifier.computeHash(script))).isTrue()
        assertThat(verifier.verifyHash(script, "invalid_hash")).isFalse()
    }

    private fun KeyPair.signedScript(): Script {
        val unsigned = testScript()
        return unsigned.copy(signature = verifier.sign(unsigned, private))
    }
}
