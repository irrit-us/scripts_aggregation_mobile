package com.scripthost.security

import com.scripthost.models.Script
import com.scripthost.models.VerificationResult
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Script Signature Verifier - Validates script integrity and authenticity
 * Uses RSA digital signatures to ensure scripts haven't been tampered with
 */
class SignatureVerifier {

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

        // In production, this would be securely embedded or fetched from a trusted source
        // For now, this is a placeholder public key
        private const val PUBLIC_KEY_BASE64 = """
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy8Dbv8prpJ/0kKhlGeJY
            ozo2t60EG8EocLo8vqGo/qhoq0eI3og8WyhmxopTShaIarKT7nBefJWmbjDSKkq0
            Y/n0cYpbH/ODdEz6zuE3lpjHGsrR5oc2FEXI1MOWspvSEm9JoS/xvscP1YO5ceKy
            1ye9WqMRHPQBG9ac2DC0lCW6r4wlmf7/fJUgMHDV155zuUZ/TSpb+W2B9N0RwQse
            K9UqRdmO5YSrCYwm+tG227hrOvFhVirvDevcdHP+EDtFOOzMJKXc9qJgs7E0imqR
            wIp9qAaLCXmSs/lvK96wZ9rwaUVGXzHrXwtkRo6vvv2Wb3j5MpCPX7P8MoqQVVsJ
            AgMBAAE=
        """
    }

    private val publicKey: PublicKey by lazy {
        val keyBytes = Base64.getDecoder().decode(PUBLIC_KEY_BASE64.replace("\\s".toRegex(), ""))
        val keySpec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    /**
     * Verify script signature
     */
    fun verify(script: Script): VerificationResult {
        // If no signature provided, check if it's required
        if (script.signature.isNullOrEmpty()) {
            return VerificationResult.Invalid("No signature provided")
        }

        return try {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey)

            // Sign the script content
            val scriptData = buildScriptData(script)
            signature.update(scriptData.toByteArray())

            // Verify signature
            val signatureBytes = Base64.getDecoder().decode(script.signature)
            val isValid = signature.verify(signatureBytes)

            if (isValid) {
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
     * Build canonical script data for signing
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
     * Generate key pair (for script authors)
     * This would be used by script developers to sign their scripts
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Sign script (for script authors)
     */
    fun sign(script: Script, privateKey: PrivateKey): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)

        val scriptData = buildScriptData(script)
        signature.update(scriptData.toByteArray())

        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    /**
     * Verify script hash (alternative to signature for local scripts)
     */
    fun verifyHash(script: Script, expectedHash: String): Boolean {
        val actualHash = computeHash(script)
        return actualHash == expectedHash
    }

    /**
     * Compute SHA-256 hash of script
     */
    fun computeHash(script: Script): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val scriptData = buildScriptData(script)
        val hashBytes = digest.digest(scriptData.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
