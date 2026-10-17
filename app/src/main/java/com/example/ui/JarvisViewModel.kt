package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiProviderManager
import com.example.audit.AuditLogger
import com.example.diagnostics.DiagnosticsEngine
import com.example.integrations.DeviceIntegrations
import com.example.model.AiConfig
import com.example.model.AuditCategory
import com.example.model.AuditEntry
import com.example.model.AuditSeverity
import com.example.model.ChatMessage
import com.example.model.CheckStatus
import com.example.model.CommandResult
import com.example.model.DiagnosticCheck
import com.example.model.MessageSender
import com.example.model.RepairOutcome
import com.example.model.TaskPlan
import com.example.permission.JarvisPermissionManager
import com.example.planner.TaskPlannerEngine
import com.example.repair.SelfRepairEngine
import com.example.setup.SetupEngine
import com.example.setup.SetupReport
import com.example.terminal.TerminalEngine
import com.example.voice.SupportedLanguage
import com.example.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class JarvisNavTab(val label: String) {
    ASSISTANT("Assistant"),
    PLANNER("Planner"),
    TERMINAL("Terminal"),
    INTEGRATIONS("Integrations"),
    DIAGNOSTICS("Diagnostics"),
    SETTINGS("Settings & Logs")
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    // Subsystem instances
    val permissionManager = JarvisPermissionManager(application)
    val deviceIntegrations = DeviceIntegrations(application, permissionManager)
    val voiceEngine = VoiceEngine(application, permissionManager)
    val terminalEngine = TerminalEngine(application, deviceIntegrations, permissionManager, voiceEngine)
    val aiProviderManager = AiProviderManager(application)
    val taskPlanner = TaskPlannerEngine(aiProviderManager, terminalEngine, permissionManager, deviceIntegrations, voiceEngine)
    val diagnosticsEngine = DiagnosticsEngine(application, deviceIntegrations, permissionManager, voiceEngine)
    val selfRepairEngine = SelfRepairEngine(application, diagnosticsEngine, deviceIntegrations, permissionManager, voiceEngine)
    val setupEngine = SetupEngine(application, deviceIntegrations, permissionManager, voiceEngine)

    // UI States
    private val _currentTab = MutableStateFlow(JarvisNavTab.ASSISTANT)
    val currentTab: StateFlow<JarvisNavTab> = _currentTab.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _terminalOutputs = MutableStateFlow<List<CommandResult>>(emptyList())
    val terminalOutputs: StateFlow<List<CommandResult>> = _terminalOutputs.asStateFlow()

    private val _diagnosticChecks = MutableStateFlow<List<DiagnosticCheck>>(emptyList())
    val diagnosticChecks: StateFlow<List<DiagnosticCheck>> = _diagnosticChecks.asStateFlow()

    private val _isDiagnosticsScanning = MutableStateFlow(false)
    val isDiagnosticsScanning: StateFlow<Boolean> = _isDiagnosticsScanning.asStateFlow()

    private val _activeRepairOutcome = MutableStateFlow<RepairOutcome?>(null)
    val activeRepairOutcome: StateFlow<RepairOutcome?> = _activeRepairOutcome.asStateFlow()

    private val _repairStepProgress = MutableStateFlow<Pair<Int, String>?>(null)
    val repairStepProgress: StateFlow<Pair<Int, String>?> = _repairStepProgress.asStateFlow()

    private val _batteryInfo = MutableStateFlow(deviceIntegrations.getBatteryInfo())
    val batteryInfo: StateFlow<DeviceIntegrations.BatteryInfo> = _batteryInfo.asStateFlow()

    private val _aiConfig = MutableStateFlow(aiProviderManager.loadConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _setupReport = MutableStateFlow<SetupReport?>(null)
    val setupReport: StateFlow<SetupReport?> = _setupReport.asStateFlow()

    private val _showSetupDialog = MutableStateFlow(false)
    val showSetupDialog: StateFlow<Boolean> = _showSetupDialog.asStateFlow()

    val auditEntries: StateFlow<List<AuditEntry>> = AuditLogger.auditFlow

    init {
        // Wire voice recognition callback
        voiceEngine.onSpeechRecognized = { recognizedText ->
            handleUserInstruction(recognizedText, isVoice = true)
        }

        // Welcome message
        addJarvisMessage("JARVIS systems initialized and operational. Multilingual speech, task planner, terminal, and device control are standing by.")

        // First launch check
        if (setupEngine.isFirstLaunch()) {
            _showSetupDialog.value = true
            triggerSetup()
        } else {
            refreshDiagnostics()
        }
    }

    fun selectTab(tab: JarvisNavTab) {
        _currentTab.value = tab
    }

    fun dismissSetupDialog() {
        setupEngine.markSetupCompleted()
        _showSetupDialog.value = false
    }

    fun triggerSetup() {
        viewModelScope.launch {
            val report = setupEngine.runAutonomousSetup(_aiConfig.value.customApiKey)
            _setupReport.value = report
            refreshDiagnostics()
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            _isDiagnosticsScanning.value = true
            _batteryInfo.value = deviceIntegrations.getBatteryInfo()
            val checks = diagnosticsEngine.runFullDiagnostics(_aiConfig.value.customApiKey)
            _diagnosticChecks.value = checks
            _isDiagnosticsScanning.value = false
        }
    }

    fun runSelfRepair(procedureId: String, userApproved: Boolean = true) {
        viewModelScope.launch {
            _repairStepProgress.value = Pair(1, "Starting procedure...")
            val outcome = selfRepairEngine.executeRepair(
                procedureId = procedureId,
                userApproved = userApproved,
                onProgress = { step, name ->
                    _repairStepProgress.value = Pair(step, name)
                }
            )
            _activeRepairOutcome.value = outcome
            _repairStepProgress.value = null
            refreshDiagnostics()
        }
    }

    fun dismissRepairOutcome() {
        _activeRepairOutcome.value = null
    }

    fun handleUserInstruction(instruction: String, isVoice: Boolean = false) {
        if (instruction.isBlank()) return

        addUserMessage(instruction)

        viewModelScope.launch {
            // Check if user is asking to plan or do complex task
            val lower = instruction.lowercase().trim()
            if (lower.startsWith("plan ") || lower.contains("set up a plan") || lower.contains("make a plan")) {
                val cleanGoal = instruction.replace(Regex("(?i)^plan|please|set up a plan for|make a plan for"), "").trim()
                val plan = taskPlanner.createPlan(cleanGoal.ifEmpty { instruction })
                addJarvisMessage(
                    text = "Autonomous execution plan generated with ${plan.steps.size} steps. Risk level: ${plan.potentialRisks}.",
                    plan = plan
                )
                if (isVoice) {
                    voiceEngine.speak("Plan generated with ${plan.steps.size} steps. Please review in the planner.")
                }
                _currentTab.value = JarvisNavTab.PLANNER
                return@launch
            }

            // Check if user input is an explicit terminal command
            val tokens = lower.split("\\s+".toRegex())
            val cmdMatch = terminalEngine.registeredCommands.any { it.name == tokens[0] }
            if (cmdMatch) {
                val result = terminalEngine.execute(instruction)
                addJarvisMessage(result.output, command = instruction)
                if (isVoice && result.output.length < 150) {
                    voiceEngine.speak(result.output)
                }
                return@launch
            }

            // Query AI Provider or Offline Core
            val response = aiProviderManager.query(instruction)
            addJarvisMessage(response)
            if (isVoice) {
                // Speak response in configured language
                voiceEngine.speak(response.take(200))
            }
        }
    }

    fun executeTerminalCommand(command: String) {
        viewModelScope.launch {
            val result = terminalEngine.execute(command)
            if (result.output == "[CLEAR]") {
                _terminalOutputs.value = emptyList()
            } else {
                _terminalOutputs.value = _terminalOutputs.value + result
            }
            _batteryInfo.value = deviceIntegrations.getBatteryInfo()
        }
    }

    fun createPlanFromInstruction(instruction: String) {
        viewModelScope.launch {
            taskPlanner.createPlan(instruction)
        }
    }

    fun executeActivePlan() {
        viewModelScope.launch {
            taskPlanner.executeActivePlan()
        }
    }

    fun approveActivePlan() {
        taskPlanner.approvePlan()
    }

    fun pausePlan() {
        taskPlanner.pause()
    }

    fun resumePlan() {
        taskPlanner.resume()
    }

    fun cancelPlan() {
        taskPlanner.cancel()
    }

    fun setLanguage(lang: SupportedLanguage) {
        voiceEngine.setLanguage(lang)
        AuditLogger.log(
            category = AuditCategory.VOICE,
            severity = AuditSeverity.INFO,
            action = "Switch Language",
            details = "Switched to ${lang.displayName}",
            outcome = "APPLIED"
        )
    }

    fun saveAiConfig(newConfig: AiConfig) {
        aiProviderManager.saveConfig(newConfig)
        _aiConfig.value = newConfig
    }

    private fun addUserMessage(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.USER,
            content = text
        )
    }

    private fun addJarvisMessage(text: String, plan: TaskPlan? = null, command: String? = null) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.JARVIS,
            content = text,
            associatedPlan = plan,
            executionCommand = command
        )
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
    }
}
