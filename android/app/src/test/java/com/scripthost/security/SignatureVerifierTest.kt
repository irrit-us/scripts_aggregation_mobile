package com.scripthost.security

import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.VerificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun signAndVerify_roundTripsWithMatchingKey() {
        val keyPair = verifier.generateKeyPair()
        val signedScript = keyPair.signedScript()

        val result = SignatureVerifier(keyPair.public).verify(signedScript)
        assertTrue(result is VerificationResult.Valid)
    }

    @Test
    fun verify_rejectsTamperedSourceCode() {
        val keyPair = verifier.generateKeyPair()
        val signature = verifier.sign(testScript(), keyPair.private)
        val tampered = testScript(sourceCode = "console.log('tampered');").copy(signature = signature)

        val result = SignatureVerifier(keyPair.public).verify(tampered)
        assertTrue(result is VerificationResult.Invalid)
    }

    @Test
    fun verify_rejectsSignatureFromDifferentKey() {
        val signingKeyPair = verifier.generateKeyPair()
        val verifyingKeyPair = verifier.generateKeyPair()

        val signature = verifier.sign(testScript(), signingKeyPair.private)
        val script = testScript().copy(signature = signature)

        val result = SignatureVerifier(verifyingKeyPair.public).verify(script)
        assertTrue(result is VerificationResult.Invalid)
    }

    @Test
    fun verify_rejectsMissingSignature() {
        val result = verifier.verify(testScript().copy(signature = null))
        assertTrue(result is VerificationResult.Invalid)
    }

    @Test
    fun computeHash_isStableForSameContent() {
        val hash1 = verifier.computeHash(testScript())
        val hash2 = verifier.computeHash(testScript())
        assertEquals(hash1, hash2)
    }

    @Test
    fun computeHash_changesWithContent() {
        val original = verifier.computeHash(testScript())
        val modified = verifier.computeHash(testScript(sourceCode = "console.log('modified');"))
        assertNotEquals(original, modified)
    }

    @Test
    fun verifyHash_detectsMismatch() {
        val script = testScript()
        assertTrue(verifier.verifyHash(script, verifier.computeHash(script)))
        assertFalse(verifier.verifyHash(script, "invalid_hash"))
    }

    private fun KeyPair.signedScript(): Script {
        val unsigned = testScript()
        return unsigned.copy(signature = verifier.sign(unsigned, private))
    }
}
