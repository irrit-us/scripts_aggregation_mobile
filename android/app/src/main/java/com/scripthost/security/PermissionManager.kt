package com.scripthost.security

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.scripthost.models.Permission
import com.scripthost.models.Script
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Permission Manager - Handles runtime permission requests and checks
 * Ensures scripts only access authorized system features
 */
class PermissionManager(private val context: Context) {

    private val grantedPermissions = ConcurrentHashMap<String, MutableSet<Permission>>()
    private val permissionCallbacks = ConcurrentHashMap<Int, (Boolean) -> Unit>()
    private val requestCodeCounter = AtomicInteger(1000)

    companion object {
        // Map script permissions to Android permissions
        private val PERMISSION_MAPPING = mapOf(
            Permission.LOCATION_FINE to android.Manifest.permission.ACCESS_FINE_LOCATION,
            Permission.LOCATION_COARSE to android.Manifest.permission.ACCESS_COARSE_LOCATION,
            Permission.CAMERA to android.Manifest.permission.CAMERA,
            Permission.RECORD_AUDIO to android.Manifest.permission.RECORD_AUDIO,
            Permission.READ_CONTACTS to android.Manifest.permission.READ_CONTACTS,
            Permission.WRITE_CONTACTS to android.Manifest.permission.WRITE_CONTACTS
        )
    }

    /**
     * Check if a script has a specific permission
     */
    fun hasPermission(scriptId: String, permission: Permission): Boolean {
        return grantedPermissions[scriptId]?.contains(permission) ?: false
    }

    /**
     * Check if a script is allowed to use a permission at runtime: it must be
     * in the script's granted set AND pass the system-level check.
     */
    fun hasScriptPermission(scriptId: String, permission: Permission): Boolean {
        val granted = grantedPermissions[scriptId]?.contains(permission) ?: false
        return granted && hasPermission(permission)
    }

    /**
     * Check if current context has a permission (for system bridge)
     */
    fun hasPermission(permission: Permission): Boolean {
        // Non-dangerous permissions are always granted, except notifications
        // on API 33+ where POST_NOTIFICATIONS is a runtime permission.
        if (!permission.dangerous) {
            if (permission != Permission.NOTIFICATIONS || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true
            }
        }

        // Check Android system permission
        val androidPermission = androidPermissionFor(permission) ?: return false
        return ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Map a script permission to its Android runtime permission.
     * NOTIFICATIONS maps to POST_NOTIFICATIONS on API 33+ only.
     */
    private fun androidPermissionFor(permission: Permission): String? {
        if (permission == Permission.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return android.Manifest.permission.POST_NOTIFICATIONS
        }
        return PERMISSION_MAPPING[permission]
    }

    /**
     * Request permissions for a script
     */
    fun requestPermissions(
        activity: Activity,
        script: Script,
        callback: (granted: Set<Permission>, denied: Set<Permission>) -> Unit
    ) {
        val requestedPermissions = script.permissions
        val granted = mutableSetOf<Permission>()
        val denied = mutableSetOf<Permission>()
        val needsRequest = mutableListOf<Permission>()

        // Separate permissions into granted, denied, and needs request
        for (permission in requestedPermissions) {
            val androidPermission = androidPermissionFor(permission)
            // Non-dangerous permissions are auto-granted unless they map to a
            // runtime permission (notifications on API 33+)
            if (!permission.dangerous && androidPermission == null) {
                granted.add(permission)
                continue
            }
            if (androidPermission != null) {
                when {
                    ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED -> {
                        granted.add(permission)
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission) -> {
                        needsRequest.add(permission)
                    }
                    else -> {
                        needsRequest.add(permission)
                    }
                }
            } else {
                // Permission doesn't require Android permission
                granted.add(permission)
            }
        }

        // Store granted permissions
        grantedPermissions[script.id] = granted

        if (needsRequest.isEmpty()) {
            callback(granted, denied)
            return
        }

        // Request Android permissions
        val requestCode = requestCodeCounter.incrementAndGet()
        val androidPermissions = needsRequest.mapNotNull { androidPermissionFor(it) }.toTypedArray()

        permissionCallbacks[requestCode] = { allGranted ->
            if (allGranted) {
                granted.addAll(needsRequest)
                grantedPermissions[script.id] = granted
            } else {
                denied.addAll(needsRequest)
            }
            callback(granted, denied)
        }

        ActivityCompat.requestPermissions(activity, androidPermissions, requestCode)
    }

    /**
     * Handle permission request result
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val callback = permissionCallbacks.remove(requestCode) ?: return
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        callback(allGranted)
    }

    /**
     * Revoke all permissions for a script
     */
    fun revokePermissions(scriptId: String) {
        grantedPermissions.remove(scriptId)
    }

    /**
     * Get granted permissions for a script
     */
    fun getGrantedPermissions(scriptId: String): Set<Permission> {
        return grantedPermissions[scriptId]?.toSet() ?: emptySet()
    }

    /**
     * Check if script permissions are valid
     */
    fun validatePermissions(script: Script): Boolean {
        // Ensure all requested permissions are valid
        return script.permissions.all { permission ->
            Permission.values().contains(permission)
        }
    }
}
