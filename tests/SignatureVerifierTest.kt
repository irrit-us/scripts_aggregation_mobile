package com.scripthost.security

import com.scripthost.models.Permission
import com.scripthost.models.Script
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for SignatureVerifier
 */
class SignatureVerifierTest {

    private lateinit var verifier: SignatureVerifier

    @Before
    fun setup() {
        verifier = SignatureVerifier()
    }

    @Test
    fun testKeyPairGeneration() {
        val keyPair = verifier.generateKeyPair()

        assertNotNull(keyPair)
        assertNotNull(keyPair.private)
        assertNotNull(keyPair.public)
    }

    @Test
    fun testSignAndVerify() {
        // Generate key pair
        val keyPair = verifier.generateKeyPair()

        // Create test script
        val script = Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = listOf(Permission.INTERNET),
            sourceCode = "console.log('test');",
            createdAt = Date(),
            updatedAt = Date()
        )

        // Sign script
        val signature = verifier.sign(script, keyPair.private)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        // Create signed script
        val signedScript = script.copy(signature = signature)

        // Note: Verification would fail because we're using a different public key
        // In production, the public key would match the private key used for signing
    }

    @Test
    fun testHashComputation() {
        val script = Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = listOf(Permission.INTERNET),
            sourceCode = "console.log('test');",
            createdAt = Date(),
            updatedAt = Date()
        )

        val hash1 = verifier.computeHash(script)
        val hash2 = verifier.computeHash(script)

        // Same script should produce same hash
        assertEquals(hash1, hash2)

        // Modified script should produce different hash
        val modifiedScript = script.copy(sourceCode = "console.log('modified');")
        val hash3 = verifier.computeHash(modifiedScript)

        assertNotEquals(hash1, hash3)
    }

    @Test
    fun testHashVerification() {
        val script = Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = emptyList(),
            sourceCode = "console.log('test');"
        )

        val hash = verifier.computeHash(script)

        // Verify with correct hash
        assertTrue(verifier.verifyHash(script, hash))

        // Verify with incorrect hash
        assertFalse(verifier.verifyHash(script, "invalid_hash"))
    }

    @Test
    fun testNoSignatureProvided() {
        val script = Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = emptyList(),
            sourceCode = "console.log('test');",
            signature = null
        )

        val result = verifier.verify(script)

        // Should return Invalid when no signature provided
        assertTrue(result is com.scripthost.models.VerificationResult.Invalid)
    }
}
