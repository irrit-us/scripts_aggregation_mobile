package com.scripthost.security

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.scripthost.models.Permission
import com.scripthost.models.Script
import java.util.concurrent.ConcurrentHashMap

/**
 * Permission Manager - Handles runtime permission requests and checks
 * Ensures scripts only access authorized system features
 */
class PermissionManager(private val context: Context) {

    private val grantedPermissions = ConcurrentHashMap<String, MutableSet<Permission>>()
    private val permissionCallbacks = mutableMapOf<Int, (Boolean) -> Unit>()
    private var requestCodeCounter = 1000

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
     * Check if current context has a permission (for system bridge)
     */
    fun hasPermission(permission: Permission): Boolean {
        // Non-dangerous permissions are always granted
        if (!permission.dangerous) {
            return true
        }

        // Check Android system permission
        val androidPermission = PERMISSION_MAPPING[permission] ?: return false
        return ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
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
            if (!permission.dangerous) {
                // Non-dangerous permissions are auto-granted
                granted.add(permission)
            } else {
                val androidPermission = PERMISSION_MAPPING[permission]
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
        }

        // Store granted permissions
        grantedPermissions[script.id] = granted

        if (needsRequest.isEmpty()) {
            callback(granted, denied)
            return
        }

        // Request Android permissions
        val requestCode = requestCodeCounter++
        val androidPermissions = needsRequest.mapNotNull { PERMISSION_MAPPING[it] }.toTypedArray()

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
