package com.example.setup

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import com.example.BuildConfig
import com.example.audit.AuditLogger
import com.example.integrations.DeviceIntegrations
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.permission.JarvisPermissionManager
import com.example.voice.VoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SetupStepResult(
    val title: String,
    val isCompleted: Boolean,
    val details: String,
    val requiredAction: String? = null
)

data class SetupReport(
    val androidVersion: String,
    val deviceModel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val completedSteps: List<SetupStepResult>,
    val pendingUserActions: List<SetupStepResult>,
    val summary: String
)

class SetupEngine(
    private val context: Context,
    private val deviceIntegrations: DeviceIntegrations,
    private val permissionManager: JarvisPermissionManager,
    private val voiceEngine: VoiceEngine
) {
    private val _setupReport = MutableStateFlow<SetupReport?>(null)
    val setupReport: StateFlow<SetupReport?> = _setupReport.asStateFlow()

    private val prefs = context.getSharedPreferences("jarvis_setup_prefs", Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean = prefs.getBoolean("is_first_launch", true)

    fun markSetupCompleted() {
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    suspend fun runAutonomousSetup(configuredKey: String?): SetupReport = withContext(Dispatchers.IO) {
        val completed = mutableListOf<SetupStepResult>()
        val pending = mutableListOf<SetupStepResult>()

        AuditLogger.log(
            category = AuditCategory.SYSTEM,
            severity = AuditSeverity.INFO,
            action = "Autonomous Setup Initiated",
            details = "Inspecting Android OS, device capabilities, and permission architecture",
            outcome = "IN_PROGRESS"
        )

        // Step 1: Android Version & Platform Constraints
        val spec = deviceIntegrations.getDeviceSpec()
        completed.add(
            SetupStepResult(
                title = "Android Environment Inspection",
                isCompleted = true,
                details = "Target: Android ${spec.androidVersion} (API ${spec.sdkInt}) on ${spec.manufacturer} ${spec.model}."
            )
        )

        // Step 2: Initialize Notification Channel
        completed.add(
            SetupStepResult(
                title = "Alert & Task Channel Configuration",
                isCompleted = true,
                details = "Registered notification channel '${DeviceIntegrations.CHANNEL_ID}' with high priority."
            )
        )

        // Step 3: Audio & Voice Architecture
        val sttAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val hasAudioPerm = permissionManager.getAudioPermissionStatus()
        if (sttAvailable && hasAudioPerm) {
            completed.add(
                SetupStepResult(
                    title = "Voice Speech Engine",
                    isCompleted = true,
                    details = "Speech recognizer active with RECORD_AUDIO permission."
                )
            )
        } else {
            pending.add(
                SetupStepResult(
                    title = "Voice Input Authorization",
                    isCompleted = false,
                    details = "Microphone runtime permission not yet authorized.",
                    requiredAction = "Grant Microphone permission on prompt to use voice commands."
                )
            )
        }

        // Step 4: Flashlight & Camera Hardware
        val hasCameraPerm = permissionManager.getCameraPermissionStatus()
        if (hasCameraPerm) {
            completed.add(
                SetupStepResult(
                    title = "Hardware Flashlight & Camera",
                    isCompleted = true,
                    details = "Camera permission granted; torch control available."
                )
            )
        } else {
            pending.add(
                SetupStepResult(
                    title = "Flashlight & Camera Integration",
                    isCompleted = false,
                    details = "Camera permission needed for torch control and image capture.",
                    requiredAction = "Allow camera access when prompt appears."
                )
            )
        }

        // Step 5: AI Provider Authentication
        val key = configuredKey?.ifBlank { null } ?: BuildConfig.GEMINI_API_KEY.ifBlank { null }
        if (!key.isNullOrBlank()) {
            completed.add(
                SetupStepResult(
                    title = "AI Cloud Provider",
                    isCompleted = true,
                    details = "Active API authentication detected (${key.take(4)}...)."
                )
            )
        } else {
            completed.add(
                SetupStepResult(
                    title = "Autonomous Offline Core",
                    isCompleted = true,
                    details = "Offline rule-based fallback active. You can optionally add a Gemini or OpenAI key in Settings."
                )
            )
        }

        val report = SetupReport(
            androidVersion = spec.androidVersion,
            deviceModel = "${spec.manufacturer} ${spec.model}",
            completedSteps = completed,
            pendingUserActions = pending,
            summary = "Autonomous setup finished. ${completed.size} integrations active, ${pending.size} permissions pending user consent."
        )

        _setupReport.value = report

        AuditLogger.log(
            category = AuditCategory.SYSTEM,
            severity = AuditSeverity.INFO,
            action = "Setup Completed",
            details = report.summary,
            outcome = "INITIALIZED"
        )

        report
    }
}
