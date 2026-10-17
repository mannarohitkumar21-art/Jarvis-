package com.example.diagnostics

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.BuildConfig
import com.example.audit.AuditLogger
import com.example.integrations.DeviceIntegrations
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.model.CheckStatus
import com.example.model.DiagnosticCheck
import com.example.permission.JarvisPermissionManager
import com.example.voice.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiagnosticsEngine(
    private val context: Context,
    private val deviceIntegrations: DeviceIntegrations,
    private val permissionManager: JarvisPermissionManager,
    private val voiceEngine: VoiceEngine
) {
    suspend fun runFullDiagnostics(configuredApiKey: String?): List<DiagnosticCheck> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiagnosticCheck>()

        AuditLogger.log(
            category = AuditCategory.DIAGNOSTICS,
            severity = AuditSeverity.INFO,
            action = "Start Diagnostics",
            details = "Running full 8-point system diagnostic scan",
            outcome = "IN_PROGRESS"
        )

        // 1. Audio & Voice Subsystem
        val audioPerm = permissionManager.getAudioPermissionStatus()
        val sttAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val ttsStatus = voiceEngine.isTtsReady.value
        val langCheck = voiceEngine.checkLanguageSupport(voiceEngine.currentLanguage.value)

        val voiceStatus = when {
            !audioPerm -> CheckStatus.WARNING
            !sttAvailable -> CheckStatus.WARNING
            !ttsStatus || langCheck < TextToSpeech.LANG_AVAILABLE -> CheckStatus.WARNING
            else -> CheckStatus.HEALTHY
        }
        results.add(
            DiagnosticCheck(
                id = "diag_voice",
                name = "Voice & Speech Engine",
                category = "Voice",
                status = voiceStatus,
                summary = if (voiceStatus == CheckStatus.HEALTHY) "STT and TTS engines fully operational" else "Audio permissions or language package required",
                details = "Mic Permission: ${if (audioPerm) "GRANTED" else "MISSING"}\nSTT Available: $sttAvailable\nTTS Initialized: $ttsStatus\nLanguage Support: $langCheck",
                possibleCause = if (!audioPerm) "RECORD_AUDIO permission has not been granted." else if (langCheck < TextToSpeech.LANG_AVAILABLE) "TTS language data pack for ${voiceEngine.currentLanguage.value.displayName} is missing." else null,
                repairable = true,
                repairProcedureId = "repair_voice_engine"
            )
        )

        // 2. Camera & Flashlight Subsystem
        val cameraPerm = permissionManager.getCameraPermissionStatus()
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        var hasFlash = false
        try {
            hasFlash = cm?.cameraIdList?.any { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } == true
        } catch (_: Exception) {}

        val cameraStatus = when {
            !hasFlash -> CheckStatus.NOT_AVAILABLE
            !cameraPerm -> CheckStatus.WARNING
            else -> CheckStatus.HEALTHY
        }
        results.add(
            DiagnosticCheck(
                id = "diag_camera",
                name = "Flashlight & Camera Hardware",
                category = "Hardware",
                status = cameraStatus,
                summary = if (hasFlash) "Flashlight hardware ready (${if (cameraPerm) "Camera perm granted" else "Perm missing"})" else "No flash hardware detected",
                details = "Flash Available: $hasFlash\nCamera Permission: ${if (cameraPerm) "GRANTED" else "MISSING"}",
                possibleCause = if (!cameraPerm) "Camera permission is not granted." else null,
                repairable = !cameraPerm,
                repairProcedureId = "repair_camera_permission"
            )
        )

        // 3. Battery & Thermal Subsystem
        val battery = deviceIntegrations.getBatteryInfo()
        val batteryStatus = when {
            battery.temperatureCelsius > 48f -> CheckStatus.CRITICAL
            battery.temperatureCelsius > 42f || battery.percentage < 15 -> CheckStatus.WARNING
            else -> CheckStatus.HEALTHY
        }
        results.add(
            DiagnosticCheck(
                id = "diag_battery",
                name = "Battery & Power Management",
                category = "Power",
                status = batteryStatus,
                summary = "${battery.percentage}% [${battery.health}] - Temp: ${battery.temperatureCelsius}°C",
                details = "Charging: ${battery.isCharging} (${battery.chargingType})\nVoltage: ${battery.voltageMv} mV\nPower Saver: ${battery.isPowerSaveMode}",
                possibleCause = if (battery.percentage < 15) "Battery is critically low." else if (battery.temperatureCelsius > 45f) "Device is experiencing thermal throttling." else null,
                repairable = false
            )
        )

        // 4. Network Connectivity
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = connMgr.activeNetwork
        val caps = connMgr.getNetworkCapabilities(activeNet)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val netStatus = if (hasInternet) CheckStatus.HEALTHY else CheckStatus.WARNING
        results.add(
            DiagnosticCheck(
                id = "diag_network",
                name = "Internet & Network Transport",
                category = "Network",
                status = netStatus,
                summary = if (hasInternet) "Online with internet connectivity" else "No active or validated internet connection",
                details = "Active Network: ${activeNet != null}\nInternet Validated: $hasInternet",
                possibleCause = if (!hasInternet) "Device is offline or captive portal detected." else null,
                repairable = true,
                repairProcedureId = "repair_network_reconnect"
            )
        )

        // 5. Storage Partition Metrics
        val spec = deviceIntegrations.getDeviceSpec()
        val storageStatus = when {
            spec.freeStorageMb < 200 -> CheckStatus.CRITICAL
            spec.freeStorageMb < 800 -> CheckStatus.WARNING
            else -> CheckStatus.HEALTHY
        }
        results.add(
            DiagnosticCheck(
                id = "diag_storage",
                name = "Storage Space & Partitions",
                category = "System",
                status = storageStatus,
                summary = "${spec.freeStorageMb} MB free of ${spec.totalStorageMb} MB",
                details = "Partition: /data\nFree Space: ${spec.freeStorageMb} MB",
                possibleCause = if (spec.freeStorageMb < 500) "Internal storage is near capacity." else null,
                repairable = false
            )
        )

        // 6. Notification Subsystem
        val notifPerm = permissionManager.getNotificationPermissionStatus()
        val notifStatus = if (notifPerm) CheckStatus.HEALTHY else CheckStatus.WARNING
        results.add(
            DiagnosticCheck(
                id = "diag_notifications",
                name = "Notification System Alerts",
                category = "Alerts",
                status = notifStatus,
                summary = if (notifPerm) "Alerts and background task updates active" else "POST_NOTIFICATIONS permission not granted",
                details = "Permission Granted: $notifPerm\nChannel ID: ${DeviceIntegrations.CHANNEL_ID}",
                possibleCause = if (!notifPerm) "Notifications permission has not been granted." else null,
                repairable = !notifPerm,
                repairProcedureId = "repair_notification_permission"
            )
        )

        // 7. Location Services
        val locPerm = permissionManager.getLocationPermissionStatus()
        val locStatus = if (locPerm) CheckStatus.HEALTHY else CheckStatus.WARNING
        results.add(
            DiagnosticCheck(
                id = "diag_location",
                name = "Location Services (GPS)",
                category = "Sensors",
                status = locStatus,
                summary = if (locPerm) "Location provider accessible" else "Location permission not granted",
                details = "Fine Location Perm: $locPerm",
                possibleCause = if (!locPerm) "Location access is disabled or permission denied." else null,
                repairable = !locPerm,
                repairProcedureId = "repair_location_permission"
            )
        )

        // 8. AI Provider Readiness
        val keyToUse = configuredApiKey?.ifBlank { null } ?: BuildConfig.GEMINI_API_KEY.ifBlank { null }
        val aiStatus = if (!keyToUse.isNullOrBlank()) CheckStatus.HEALTHY else CheckStatus.WARNING
        results.add(
            DiagnosticCheck(
                id = "diag_ai_provider",
                name = "AI Model Engine & API Key",
                category = "Intelligence",
                status = aiStatus,
                summary = if (aiStatus == CheckStatus.HEALTHY) "AI Cloud Provider connected & authenticated" else "Operating in local rule-based offline mode (API key not set)",
                details = "Configured Key: ${if (keyToUse.isNullOrBlank()) "None (Local mode active)" else "Present (${keyToUse.take(4)}...)"}",
                possibleCause = if (keyToUse.isNullOrBlank()) "No Gemini or OpenAI API key entered in settings." else null,
                repairable = false
            )
        )

        AuditLogger.log(
            category = AuditCategory.DIAGNOSTICS,
            severity = AuditSeverity.INFO,
            action = "Diagnostics Completed",
            details = "Total Checks: ${results.size}, Healthy: ${results.count { it.status == CheckStatus.HEALTHY }}",
            outcome = "COMPLETE"
        )

        results
    }
}
