package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiProviderManager
import com.example.audit.AuditLogger
import com.example.diagnostics.DiagnosticsEngine
import com.example.integrations.DeviceIntegrations
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.model.CheckStatus
import com.example.model.CommandStatus
import com.example.model.RiskLevel
import com.example.permission.JarvisPermissionManager
import com.example.planner.TaskPlannerEngine
import com.example.repair.SelfRepairEngine
import com.example.setup.SetupEngine
import com.example.terminal.TerminalEngine
import com.example.voice.SupportedLanguage
import com.example.voice.VoiceEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var permissionManager: JarvisPermissionManager
    private lateinit var deviceIntegrations: DeviceIntegrations
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var terminalEngine: TerminalEngine
    private lateinit var aiProviderManager: AiProviderManager
    private lateinit var taskPlanner: TaskPlannerEngine
    private lateinit var diagnosticsEngine: DiagnosticsEngine
    private lateinit var selfRepairEngine: SelfRepairEngine
    private lateinit var setupEngine: SetupEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        permissionManager = JarvisPermissionManager(context)
        deviceIntegrations = DeviceIntegrations(context, permissionManager)
        voiceEngine = VoiceEngine(context, permissionManager)
        terminalEngine = TerminalEngine(context, deviceIntegrations, permissionManager, voiceEngine)
        aiProviderManager = AiProviderManager(context)
        taskPlanner = TaskPlannerEngine(aiProviderManager, terminalEngine, permissionManager, deviceIntegrations, voiceEngine)
        diagnosticsEngine = DiagnosticsEngine(context, deviceIntegrations, permissionManager, voiceEngine)
        selfRepairEngine = SelfRepairEngine(context, diagnosticsEngine, deviceIntegrations, permissionManager, voiceEngine)
        setupEngine = SetupEngine(context, deviceIntegrations, permissionManager, voiceEngine)
    }

    @Test
    fun testAppNameIsJarvisAI() {
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun testTerminalHelpAndStatus() = runBlocking {
        val helpResult = terminalEngine.execute("help")
        assertEquals(0, helpResult.returnCode)
        assertTrue(helpResult.output.contains("JARVIS AUTONOMOUS TERMINAL"))

        val statusResult = terminalEngine.execute("status")
        assertEquals(0, statusResult.returnCode)
        assertTrue(statusResult.output.contains("[JARVIS CORE TELEMETRY]"))

        val unknownResult = terminalEngine.execute("nonexistent_cmd")
        assertEquals(127, unknownResult.returnCode)
        assertEquals(CommandStatus.FAILED, unknownResult.status)
    }

    @Test
    fun testTerminalBatteryAndStorage() = runBlocking {
        val batteryResult = terminalEngine.execute("battery")
        assertEquals(0, batteryResult.returnCode)
        assertTrue(batteryResult.output.contains("BATTERY SUBSYSTEM"))

        val storageResult = terminalEngine.execute("storage")
        assertEquals(0, storageResult.returnCode)
        assertTrue(storageResult.output.contains("INTERNAL STORAGE PARTITION"))
    }

    @Test
    fun testTaskPlannerFormulation() = runBlocking {
        val plan = taskPlanner.createPlan("Turn on the flashlight and check battery")
        assertNotNull(plan)
        assertEquals(RiskLevel.LOW, plan.potentialRisks)
        assertTrue(plan.steps.isNotEmpty())
        assertTrue(plan.requiredTools.contains("torch"))

        val alarmPlan = taskPlanner.createPlan("Schedule wake up alarm at 07:00")
        assertEquals(RiskLevel.MEDIUM, alarmPlan.potentialRisks)
        assertFalse(alarmPlan.isApprovedByHuman)
    }

    @Test
    fun testAuditLoggerSanitization() {
        AuditLogger.clear()
        val leakedKey = "AIzaSyD-fakeKeyTestSecret1234567890"
        AuditLogger.log(
            category = AuditCategory.SYSTEM,
            severity = AuditSeverity.SECURITY,
            action = "API Key Test",
            details = "Supplied key: $leakedKey",
            outcome = "TESTED"
        )

        val recent = AuditLogger.getRecent(1)
        assertEquals(1, recent.size)
        assertFalse(recent[0].details.contains("AIzaSyD"))
        assertTrue(recent[0].details.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun testAutonomousSetupEngineReport() = runBlocking {
        val report = setupEngine.runAutonomousSetup(null)
        assertNotNull(report)
        assertTrue(report.completedSteps.isNotEmpty())
        assertNotNull(report.summary)
    }

    @Test
    fun testSelfRepairEngineReinitialization() = runBlocking {
        val outcome = selfRepairEngine.executeRepair("repair_voice_engine", userApproved = true)
        assertTrue(outcome.success)
        assertEquals(7, outcome.stepResults.size)
    }

    @Test
    fun testOfflineCoreMultilingualGreeting() {
        val bnGreeting = aiProviderManager.queryOfflineCore("হ্যালো জারভিস কেমন আছো")
        assertTrue(bnGreeting.contains("জারভিস") || bnGreeting.contains("JARVIS"))

        val hiGreeting = aiProviderManager.queryOfflineCore("नमस्ते जार्विस")
        assertTrue(hiGreeting.contains("जार्विस") || hiGreeting.contains("JARVIS"))

        val enGreeting = aiProviderManager.queryOfflineCore("hello jarvis")
        assertTrue(enGreeting.contains("JARVIS autonomous systems are online"))
    }
}
