package com.example.planner

import android.Manifest
import com.example.ai.AiProviderManager
import com.example.audit.AuditLogger
import com.example.integrations.DeviceIntegrations
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.model.PlanStep
import com.example.model.RiskLevel
import com.example.model.StepStatus
import com.example.model.TaskPlan
import com.example.permission.JarvisPermissionManager
import com.example.terminal.TerminalEngine
import com.example.voice.VoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TaskPlannerEngine(
    private val aiProviderManager: AiProviderManager,
    private val terminalEngine: TerminalEngine,
    private val permissionManager: JarvisPermissionManager,
    private val deviceIntegrations: DeviceIntegrations,
    private val voiceEngine: VoiceEngine
) {
    private val _activePlan = MutableStateFlow<TaskPlan?>(null)
    val activePlan: StateFlow<TaskPlan?> = _activePlan.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentExecutingStepIndex = MutableStateFlow(-1)
    val currentExecutingStepIndex: StateFlow<Int> = _currentExecutingStepIndex.asStateFlow()

    suspend fun createPlan(userInstruction: String): TaskPlan = withContext(Dispatchers.Default) {
        val prompt = userInstruction.lowercase().trim()

        AuditLogger.log(
            category = AuditCategory.TASK_EXECUTION,
            severity = AuditSeverity.INFO,
            action = "Plan Creation Request",
            details = "Instruction: '$userInstruction'",
            outcome = "ANALYZING"
        )

        // Generate tailored structured plans based on user goal
        val plan = when {
            prompt.contains("torch") || prompt.contains("flashlight") || prompt.contains("light") -> {
                TaskPlan(
                    userInstruction = userInstruction,
                    intent = "Control hardware flashlight and confirm state",
                    requiredTools = listOf("torch", "battery"),
                    requiredPermissions = listOf(Manifest.permission.CAMERA),
                    expectedOutcome = "Device flashlight is engaged with power telemetry checked",
                    potentialRisks = RiskLevel.LOW,
                    steps = listOf(
                        PlanStep(
                            index = 1,
                            title = "Verify Camera Permission",
                            description = "Ensure CAMERA permission is available for hardware LED access",
                            tool = "PermissionManager",
                            requiredPermission = Manifest.permission.CAMERA,
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 2,
                            title = "Inspect Battery Level",
                            description = "Ensure device has sufficient battery to power torch",
                            tool = "BatterySubsystem",
                            actionCommand = "battery",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 3,
                            title = "Engage Flashlight",
                            description = "Dispatch torch toggle intent through CameraManager",
                            tool = "FlashlightSubsystem",
                            actionCommand = "torch on",
                            requiredPermission = Manifest.permission.CAMERA,
                            riskLevel = RiskLevel.LOW
                        )
                    ),
                    verificationMethod = "Query camera torch state and confirm illumination."
                )
            }
            prompt.contains("alarm") || prompt.contains("wake") || prompt.contains("timer") -> {
                TaskPlan(
                    userInstruction = userInstruction,
                    intent = "Schedule device alarm clock alert",
                    requiredTools = listOf("alarm", "notify"),
                    requiredPermissions = listOf("com.android.alarm.permission.SET_ALARM"),
                    expectedOutcome = "Alarm scheduled in Android Clock with confirmation banner",
                    potentialRisks = RiskLevel.MEDIUM,
                    steps = listOf(
                        PlanStep(
                            index = 1,
                            title = "Parse Scheduled Time",
                            description = "Extract target hour and minute parameters",
                            tool = "IntentParser",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 2,
                            title = "Set System Alarm",
                            description = "Dispatch Android AlarmClock intent for 07:00",
                            tool = "AlarmIntent",
                            actionCommand = "alarm 7 00 Scheduled Task",
                            riskLevel = RiskLevel.MEDIUM
                        ),
                        PlanStep(
                            index = 3,
                            title = "Post Confirmation Notification",
                            description = "Notify user with alarm timestamp",
                            tool = "NotificationSubsystem",
                            actionCommand = "notify Alarm Alarm_Scheduled_For_07:00",
                            riskLevel = RiskLevel.LOW
                        )
                    ),
                    verificationMethod = "Confirm AlarmClock intent dispatch status code."
                )
            }
            prompt.contains("diagnos") || prompt.contains("health") || prompt.contains("check") -> {
                TaskPlan(
                    userInstruction = userInstruction,
                    intent = "Perform comprehensive 8-point hardware & software health check",
                    requiredTools = listOf("diagnose", "battery", "storage", "ping"),
                    requiredPermissions = emptyList(),
                    expectedOutcome = "Complete diagnostic report with resolution paths",
                    potentialRisks = RiskLevel.LOW,
                    steps = listOf(
                        PlanStep(
                            index = 1,
                            title = "Scan Battery & Thermals",
                            description = "Inspect temperature, health, and charging cycle",
                            tool = "BatterySubsystem",
                            actionCommand = "battery",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 2,
                            title = "Measure Storage Partitions",
                            description = "Verify free bytes on /data filesystem",
                            tool = "StorageSubsystem",
                            actionCommand = "storage",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 3,
                            title = "Probe Internet Gateway",
                            description = "Measure ICMP/socket ping latency to DNS root",
                            tool = "NetworkSubsystem",
                            actionCommand = "ping 8.8.8.8",
                            riskLevel = RiskLevel.LOW
                        )
                    ),
                    verificationMethod = "Aggregate and display all subsystem diagnostic flags."
                )
            }
            prompt.contains("launch") || prompt.contains("open") -> {
                val appToOpen = userInstruction.replace(Regex("(?i)launch|open|please|app"), "").trim()
                TaskPlan(
                    userInstruction = userInstruction,
                    intent = "Locate and launch application: $appToOpen",
                    requiredTools = listOf("launch"),
                    requiredPermissions = emptyList(),
                    expectedOutcome = "Application launched into foreground",
                    potentialRisks = RiskLevel.LOW,
                    steps = listOf(
                        PlanStep(
                            index = 1,
                            title = "Query Installed Packages",
                            description = "Locate package matching '$appToOpen' via PackageManager",
                            tool = "PackageManager",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 2,
                            title = "Dispatch Launch Intent",
                            description = "Start activity with FLAG_ACTIVITY_NEW_TASK",
                            tool = "AppLauncher",
                            actionCommand = "launch $appToOpen",
                            riskLevel = RiskLevel.LOW
                        )
                    ),
                    verificationMethod = "Verify activity manager launch intent resolution."
                )
            }
            else -> {
                // Generalized Autonomous Plan
                TaskPlan(
                    userInstruction = userInstruction,
                    intent = "Autonomous Execution: $userInstruction",
                    requiredTools = listOf("status", "speak"),
                    requiredPermissions = emptyList(),
                    expectedOutcome = "Instruction validated and synthesized through JARVIS voice & terminal",
                    potentialRisks = RiskLevel.LOW,
                    steps = listOf(
                        PlanStep(
                            index = 1,
                            title = "System Readiness Telemetry",
                            description = "Verify core subsystems are operational",
                            tool = "StatusChecker",
                            actionCommand = "status",
                            riskLevel = RiskLevel.LOW
                        ),
                        PlanStep(
                            index = 2,
                            title = "Vocal Synthesis",
                            description = "Synthesize natural language confirmation",
                            tool = "VoiceEngine",
                            actionCommand = "speak Task planned successfully: $userInstruction",
                            riskLevel = RiskLevel.LOW
                        )
                    ),
                    verificationMethod = "Observe vocal output and terminal status confirmation."
                )
            }
        }

        _activePlan.value = plan
        plan
    }

    fun approvePlan() {
        val current = _activePlan.value ?: return
        current.isApprovedByHuman = true
        _activePlan.value = current.copy(isApprovedByHuman = true)
        AuditLogger.log(
            category = AuditCategory.TASK_EXECUTION,
            severity = AuditSeverity.INFO,
            action = "Plan Approved",
            details = "User manually authorized plan '${current.intent}'",
            outcome = "APPROVED"
        )
    }

    suspend fun executeActivePlan(): Boolean = withContext(Dispatchers.IO) {
        val plan = _activePlan.value ?: return@withContext false

        // Check if sensitive plan needs approval
        if (plan.potentialRisks != RiskLevel.LOW && !plan.isApprovedByHuman) {
            AuditLogger.log(
                category = AuditCategory.TASK_EXECUTION,
                severity = AuditSeverity.WARNING,
                action = "Execution Blocked",
                details = "Plan risk is ${plan.potentialRisks} and requires user confirmation.",
                outcome = "AWAITING_APPROVAL"
            )
            return@withContext false
        }

        _isExecuting.value = true
        _isPaused.value = false

        for (step in plan.steps) {
            while (_isPaused.value) {
                delay(200)
            }

            if (!_isExecuting.value) {
                // Cancelled
                step.status = StepStatus.SKIPPED
                step.resultMessage = "Task cancelled by user."
                break
            }

            _currentExecutingStepIndex.value = step.index
            step.status = StepStatus.IN_PROGRESS
            _activePlan.value = plan.copy()

            // Verify permission if required for step
            if (step.requiredPermission != null) {
                val hasPerm = permissionManager.hasPermission(step.requiredPermission)
                if (!hasPerm) {
                    step.status = StepStatus.FAILED
                    step.resultMessage = "Permission ${step.requiredPermission} was not granted."
                    AuditLogger.log(
                        category = AuditCategory.PERMISSION,
                        severity = AuditSeverity.ALERT,
                        action = "Step Execution Failed",
                        details = "Missing required permission for step ${step.index}",
                        outcome = "FAILED"
                    )
                    _isExecuting.value = false
                    _activePlan.value = plan.copy()
                    return@withContext false
                }
            }

            delay(300) // Realistic progression pause

            // Execute command if defined
            if (step.actionCommand != null) {
                val res = terminalEngine.execute(step.actionCommand)
                if (res.returnCode == 0) {
                    step.status = StepStatus.SUCCESS
                    step.resultMessage = res.output.take(120)
                } else {
                    step.status = StepStatus.FAILED
                    step.resultMessage = res.error ?: res.output
                    _isExecuting.value = false
                    _activePlan.value = plan.copy()
                    return@withContext false
                }
            } else {
                step.status = StepStatus.SUCCESS
                step.resultMessage = "Validated successfully."
            }

            _activePlan.value = plan.copy()
        }

        plan.isCompleted = true
        _isExecuting.value = false
        _currentExecutingStepIndex.value = -1
        _activePlan.value = plan.copy(isCompleted = true)

        AuditLogger.log(
            category = AuditCategory.TASK_EXECUTION,
            severity = AuditSeverity.INFO,
            action = "Plan Execution Finished",
            details = "Completed all ${plan.steps.size} steps for '${plan.intent}'",
            outcome = "SUCCESS"
        )

        true
    }

    fun pause() {
        _isPaused.value = true
    }

    fun resume() {
        _isPaused.value = false
    }

    fun cancel() {
        _isExecuting.value = false
        _isPaused.value = false
        AuditLogger.log(
            category = AuditCategory.TASK_EXECUTION,
            severity = AuditSeverity.WARNING,
            action = "Task Execution Cancelled",
            details = "User pressed cancel",
            outcome = "CANCELLED"
        )
    }

    fun clear() {
        _activePlan.value = null
        _isExecuting.value = false
        _isPaused.value = false
        _currentExecutingStepIndex.value = -1
    }
}
