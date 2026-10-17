package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JarvisNavTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorHeader
import com.example.ui.components.SetupDialog
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.SettingsAuditScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisObsidianBg
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                JarvisApp()
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val terminalOutputs by viewModel.terminalOutputs.collectAsStateWithLifecycle()
    val diagnosticChecks by viewModel.diagnosticChecks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isDiagnosticsScanning.collectAsStateWithLifecycle()
    val repairOutcome by viewModel.activeRepairOutcome.collectAsStateWithLifecycle()
    val repairStepProgress by viewModel.repairStepProgress.collectAsStateWithLifecycle()
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val aiConfig by viewModel.aiConfig.collectAsStateWithLifecycle()
    val setupReport by viewModel.setupReport.collectAsStateWithLifecycle()
    val showSetupDialog by viewModel.showSetupDialog.collectAsStateWithLifecycle()
    val auditEntries by viewModel.auditEntries.collectAsStateWithLifecycle()

    val isListening by viewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val soundRmsDb by viewModel.voiceEngine.soundRmsDb.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.voiceEngine.currentLanguage.collectAsStateWithLifecycle()
    val voiceError by viewModel.voiceEngine.voiceError.collectAsStateWithLifecycle()

    val activePlan by viewModel.taskPlanner.activePlan.collectAsStateWithLifecycle()
    val isExecutingPlan by viewModel.taskPlanner.isExecuting.collectAsStateWithLifecycle()
    val isPausedPlan by viewModel.taskPlanner.isPaused.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.taskPlanner.currentExecutingStepIndex.collectAsStateWithLifecycle()

    // Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDiagnostics()
    }

    // Request necessary core runtime permissions on start
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    // Handle back button: return to Assistant tab first before exiting
    BackHandler(enabled = currentTab != JarvisNavTab.ASSISTANT) {
        viewModel.selectTab(JarvisNavTab.ASSISTANT)
    }

    // First Launch Autonomous Setup Report Dialog
    if (showSetupDialog) {
        SetupDialog(
            report = setupReport,
            onDismiss = { viewModel.dismissSetupDialog() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisObsidianBg,
        topBar = {
            ArcReactorHeader(
                batteryInfo = batteryInfo,
                currentLanguage = currentLanguage,
                isListening = isListening,
                isSpeaking = isSpeaking,
                soundRmsDb = soundRmsDb,
                onVoiceClick = {
                    if (isListening) {
                        viewModel.voiceEngine.stopListening()
                    } else {
                        val started = viewModel.voiceEngine.startListening()
                        if (!started) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    }
                },
                onStopVoice = { viewModel.voiceEngine.stopSpeaking() },
                onHeaderClick = { viewModel.selectTab(JarvisNavTab.DIAGNOSTICS) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisSurfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder)
            ) {
                val navItems = listOf(
                    Pair(JarvisNavTab.ASSISTANT, Icons.Default.Chat),
                    Pair(JarvisNavTab.PLANNER, Icons.Default.FormatListBulleted),
                    Pair(JarvisNavTab.TERMINAL, Icons.Default.Terminal),
                    Pair(JarvisNavTab.INTEGRATIONS, Icons.Default.Bolt),
                    Pair(JarvisNavTab.DIAGNOSTICS, Icons.Default.Healing),
                    Pair(JarvisNavTab.SETTINGS, Icons.Default.Settings)
                )

                navItems.forEach { (tab, icon) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = JarvisCyan,
                            selectedTextColor = JarvisCyan,
                            unselectedIconColor = JarvisTextDim,
                            unselectedTextColor = JarvisTextDim,
                            indicatorColor = JarvisCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(JarvisObsidianBg)
        ) {
            when (currentTab) {
                JarvisNavTab.ASSISTANT -> AssistantScreen(
                    messages = chatMessages,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    voiceError = voiceError,
                    currentLanguage = currentLanguage,
                    onSendMessage = { instruction ->
                        viewModel.handleUserInstruction(instruction)
                    },
                    onVoiceToggle = {
                        if (isListening) {
                            viewModel.voiceEngine.stopListening()
                        } else {
                            val started = viewModel.voiceEngine.startListening()
                            if (!started) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            }
                        }
                    },
                    onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                    onOpenPlan = { viewModel.selectTab(JarvisNavTab.PLANNER) }
                )

                JarvisNavTab.PLANNER -> PlannerScreen(
                    activePlan = activePlan,
                    isExecuting = isExecutingPlan,
                    isPaused = isPausedPlan,
                    currentStepIndex = currentStepIndex,
                    onCreatePlan = { instruction -> viewModel.createPlanFromInstruction(instruction) },
                    onApprovePlan = { viewModel.approveActivePlan() },
                    onExecutePlan = { viewModel.executeActivePlan() },
                    onPausePlan = { viewModel.pausePlan() },
                    onResumePlan = { viewModel.resumePlan() },
                    onCancelPlan = { viewModel.cancelPlan() }
                )

                JarvisNavTab.TERMINAL -> TerminalScreen(
                    outputs = terminalOutputs,
                    onExecuteCommand = { cmd -> viewModel.executeTerminalCommand(cmd) }
                )

                JarvisNavTab.INTEGRATIONS -> IntegrationsScreen(
                    deviceIntegrations = viewModel.deviceIntegrations,
                    permissionManager = viewModel.permissionManager
                )

                JarvisNavTab.DIAGNOSTICS -> DiagnosticsScreen(
                    checks = diagnosticChecks,
                    isScanning = isScanning,
                    repairOutcome = repairOutcome,
                    repairStepProgress = repairStepProgress,
                    onScanClick = { viewModel.refreshDiagnostics() },
                    onRunRepair = { procId -> viewModel.runSelfRepair(procId) },
                    onDismissOutcome = { viewModel.dismissRepairOutcome() }
                )

                JarvisNavTab.SETTINGS -> SettingsAuditScreen(
                    aiConfig = aiConfig,
                    currentLanguage = currentLanguage,
                    auditLogs = auditEntries,
                    setupReport = setupReport,
                    onSaveAiConfig = { cfg -> viewModel.saveAiConfig(cfg) },
                    onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                    onTriggerSetup = { viewModel.triggerSetup() }
                )
            }
        }
    }
}
