package com.scripthost.models

import java.util.Date

/**
 * Represents a script package with metadata
 */
data class Script(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val permissions: List<Permission>,
    val sourceCode: String,
    val signature: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val iconUrl: String? = null,
    val category: ScriptCategory = ScriptCategory.UTILITY
)

/**
 * Script categories for organization
 */
enum class ScriptCategory {
    UTILITY,
    PRODUCTIVITY,
    ENTERTAINMENT,
    SYSTEM,
    NETWORK,
    UI_DEMO,
    OTHER
}

/**
 * Permissions that scripts can request
 */
enum class Permission(val description: String, val dangerous: Boolean) {
    // Network
    INTERNET("Access internet", false),
    NETWORK_STATE("Check network state", false),

    // Storage
    READ_STORAGE("Read files", false),
    WRITE_STORAGE("Write files", false),

    // Location
    LOCATION_FINE("Access precise location", true),
    LOCATION_COARSE("Access approximate location", true),

    // Camera & Media
    CAMERA("Use camera", true),
    RECORD_AUDIO("Record audio", true),

    // Sensors
    ACCELEROMETER("Access accelerometer", false),
    GYROSCOPE("Access gyroscope", false),

    // System
    VIBRATE("Vibrate device", false),
    NOTIFICATIONS("Show notifications", false),

    // Contacts
    READ_CONTACTS("Read contacts", true),
    WRITE_CONTACTS("Modify contacts", true);

    companion object {
        fun fromString(name: String): Permission? {
            return values().find { it.name == name }
        }
    }
}

/**
 * Script execution state
 */
enum class ScriptState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    ERROR
}

/**
 * Script execution context
 */
data class ScriptContext(
    val script: Script,
    var state: ScriptState = ScriptState.IDLE,
    val grantedPermissions: MutableSet<Permission> = mutableSetOf(),
    var errorMessage: String? = null,
    var startTime: Long = 0,
    var endTime: Long = 0
)

/**
 * Script installation result
 */
sealed class InstallResult {
    data class Success(val script: Script) : InstallResult()
    data class Failure(val error: String) : InstallResult()
}

/**
 * Script signature verification result
 */
sealed class VerificationResult {
    object Valid : VerificationResult()
    data class Invalid(val reason: String) : VerificationResult()
}
