package com.scripthost.security

import com.scripthost.models.Script
import com.scripthost.models.VerificationResult
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Script Signature Verifier - Validates script integrity and authenticity.
 * Uses RSA digital signatures to ensure scripts haven't been tampered with.
 *
 * @param publicKey the trusted key used to verify signatures. Defaults to the
 * platform key embedded below; tests and third-party verifiers can inject
 * their own key so that sign/verify round-trips are consistent.
 */
class SignatureVerifier(
    private val publicKey: PublicKey = DEFAULT_PUBLIC_KEY
) {

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

        // Platform public key. The matching private key is held by the
        // distribution channel and is never embedded in the app.
        private const val PUBLIC_KEY_BASE64 = """
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzCWx3BAbv/NNddgrsnqe
            lTa0QTCv3gQrWfj66YCk2IQ7uGLpPjVnzdLlM1SSKHXmDskP0viiIRf7qenkZx0I
            9YWdO3yj5q7kqAYFmik27EJV8x5McNUm//XZhmbzXI9TrvfdIKjwOZpAuMeyhsat
            qMyNoklR7FhHU3y9243Dot8V3TrqybUoFCkctAgYsVSFftFtjRSbGeZT5IpsC1rw
            vNrcxYF79jeVac05yZnRngY1ws6Gx+MQE0aRlB5DRnGU9CjrYNjoXeJCDsceSkb9
            9iQPzkMZhJjaAKocbXFTdNUwvLaCRHccprL8Ch15LbdMPibyU0OpWHk7632q+mcC
            2QIDAQAB
        """

        private val DEFAULT_PUBLIC_KEY: PublicKey by lazy {
            val keyBytes = Base64.getDecoder().decode(PUBLIC_KEY_BASE64.replace("\\s".toRegex(), ""))
            val keySpec = X509EncodedKeySpec(keyBytes)
            KeyFactory.getInstance("RSA").generatePublic(keySpec)
        }
    }

    /**
     * Verify script signature against the verifier's trusted public key.
     */
    fun verify(script: Script): VerificationResult = verify(script, publicKey)

    /**
     * Verify script signature against an explicit public key.
     */
    fun verify(script: Script, publicKey: PublicKey): VerificationResult {
        if (script.signature.isNullOrEmpty()) {
            return VerificationResult.Invalid("No signature provided")
        }

        return try {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey)
            signature.update(buildScriptData(script).toByteArray(Charsets.UTF_8))

            val signatureBytes = Base64.getDecoder().decode(script.signature)
            if (signature.verify(signatureBytes)) {
                VerificationResult.Valid
            } else {
                VerificationResult.Invalid("Signature verification failed")
            }
        } catch (e: SignatureException) {
            VerificationResult.Invalid("Invalid signature format: ${e.message}")
        } catch (e: Exception) {
            VerificationResult.Invalid("Verification error: ${e.message}")
        }
    }

    /**
     * Build canonical script data for signing.
     */
    private fun buildScriptData(script: Script): String {
        return buildString {
            append(script.id)
            append("|")
            append(script.name)
            append("|")
            append(script.version)
            append("|")
            append(script.author)
            append("|")
            append(script.sourceCode)
        }
    }

    /**
     * Generate key pair (for script authors).
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Sign script (for script authors).
     */
    fun sign(script: Script, privateKey: PrivateKey): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(buildScriptData(script).toByteArray(Charsets.UTF_8))

        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    /**
     * Verify script hash (alternative to signature for local scripts).
     */
    fun verifyHash(script: Script, expectedHash: String): Boolean {
        return computeHash(script) == expectedHash
    }

    /**
     * Compute SHA-256 hash of script.
     */
    fun computeHash(script: Script): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(buildScriptData(script).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
