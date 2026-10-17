package com.example.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.audit.AuditLogger
import com.example.model.AuditCategory
import com.example.model.AuditSeverity

class JarvisPermissionManager(private val context: Context) {

    data class PermissionDetail(
        val permission: String,
        val title: String,
        val rationale: String,
        val isGranted: Boolean,
        val isRequiredForCore: Boolean = false
    )

    fun hasPermission(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getAudioPermissionStatus(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)
    fun getCameraPermissionStatus(): Boolean = hasPermission(Manifest.permission.CAMERA)
    fun getLocationPermissionStatus(): Boolean = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getNotificationPermissionStatus(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    fun getAllTrackedPermissions(): List<PermissionDetail> {
        val list = mutableListOf(
            PermissionDetail(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone (Voice Engine)",
                rationale = "Allows JARVIS to listen to voice instructions in English, Bengali, and Hindi.",
                isGranted = getAudioPermissionStatus(),
                isRequiredForCore = false
            ),
            PermissionDetail(
                permission = Manifest.permission.CAMERA,
                title = "Camera & Flashlight",
                rationale = "Allows JARVIS to toggle the device torch / flashlight and trigger quick camera captures.",
                isGranted = getCameraPermissionStatus(),
                isRequiredForCore = false
            ),
            PermissionDetail(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                title = "Precise Location",
                rationale = "Enables location-aware query context (weather, distance, coordinates).",
                isGranted = getLocationPermissionStatus(),
                isRequiredForCore = false
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionDetail(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "System Notifications",
                    rationale = "Enables JARVIS to alert you when background tasks or autonomous plans complete.",
                    isGranted = getNotificationPermissionStatus(),
                    isRequiredForCore = false
                )
            )
        }
        return list
    }

    fun checkAndAudit(permission: String, actionName: String): Boolean {
        val granted = hasPermission(permission)
        AuditLogger.log(
            category = AuditCategory.PERMISSION,
            severity = if (granted) AuditSeverity.INFO else AuditSeverity.WARNING,
            action = "Check Permission: $permission",
            details = "Action '$actionName' requires $permission. Status: ${if (granted) "GRANTED" else "DENIED"}",
            outcome = if (granted) "AUTHORIZED" else "ACCESS_BLOCKED"
        )
        return granted
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        AuditLogger.log(
            category = AuditCategory.PERMISSION,
            severity = AuditSeverity.INFO,
            action = "Open App Settings",
            details = "Redirected user to Android Settings to manage permissions",
            outcome = "INTENT_LAUNCHED"
        )
    }
}
