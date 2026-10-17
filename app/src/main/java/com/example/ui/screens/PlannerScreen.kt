package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlanStep
import com.example.model.RiskLevel
import com.example.model.StepStatus
import com.example.model.TaskPlan
import com.example.ui.theme.JarvisAlertRed
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisNeonPurple
import com.example.ui.theme.JarvisObsidianBg
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceCardHigh
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextWhite
import com.example.ui.theme.JarvisWarningAmber

@Composable
fun PlannerScreen(
    activePlan: TaskPlan?,
    isExecuting: Boolean,
    isPaused: Boolean,
    currentStepIndex: Int,
    onCreatePlan: (String) -> Unit,
    onApprovePlan: () -> Unit,
    onExecutePlan: () -> Unit,
    onPausePlan: () -> Unit,
    onResumePlan: () -> Unit,
    onCancelPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customInstruction by remember { mutableStateOf("") }

    val presetPlans = listOf(
        "Configure night mode, battery check & torch",
        "Diagnose storage, network & voice engines",
        "Schedule wake up alarm at 07:00 with alert",
        "Launch system settings and verify status"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Card
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTONOMOUS TASK PLANNER",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (activePlan != null) "PLAN ACTIVE" else "IDLE",
                                color = JarvisCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customInstruction,
                        onValueChange = { customInstruction = it },
                        placeholder = {
                            Text(
                                text = "Enter autonomous task (e.g., 'Turn on torch and check battery')",
                                color = JarvisTextDim,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("planner_instruction_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextWhite,
                            unfocusedTextColor = JarvisTextWhite,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisSurfaceBorder,
                            focusedContainerColor = JarvisObsidianBg,
                            unfocusedContainerColor = JarvisObsidianBg
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (customInstruction.isNotBlank()) {
                                    onCreatePlan(customInstruction)
                                    customInstruction = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidianBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("planner_create_button")
                        ) {
                            Text("Formulate Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Or pick an autonomous recipe:",
                        color = JarvisTextDim,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetPlans) { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JarvisSurfaceCardHigh)
                                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                                    .clickable { onCreatePlan(preset) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset,
                                    color = JarvisCyanLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Plan Dashboard
        if (activePlan != null) {
            item {
                Surface(
                    color = JarvisSurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisNeonPurple, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE TASK DASHBOARD",
                                color = JarvisNeonPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            RiskBadge(level = activePlan.potentialRisks)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = activePlan.intent,
                            color = JarvisTextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Expected Outcome: ${activePlan.expectedOutcome}",
                            color = JarvisTextDim,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Verification: ${activePlan.verificationMethod}",
                            color = JarvisTextDim,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Risk Confirmation Banner
                        if (activePlan.potentialRisks != RiskLevel.LOW && !activePlan.isApprovedByHuman) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = JarvisWarningAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, JarvisWarningAmber, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Security Alert",
                                            tint = JarvisWarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Plan includes sensitive device actions. User authorization required.",
                                            color = JarvisWarningAmber,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = onApprovePlan,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = JarvisWarningAmber,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("planner_approve_button")
                                    ) {
                                        Text("Authorize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Execution Controls
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isExecuting && !activePlan.isCompleted) {
                                Button(
                                    onClick = onExecutePlan,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JarvisCyan,
                                        contentColor = JarvisObsidianBg
                                    ),
                                    enabled = activePlan.potentialRisks == RiskLevel.LOW || activePlan.isApprovedByHuman,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("planner_execute_plan_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Execute Plan", fontWeight = FontWeight.Bold)
                                }
                            } else if (isExecuting) {
                                Button(
                                    onClick = if (isPaused) onResumePlan else onPausePlan,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPaused) JarvisSuccessGreen else JarvisWarningAmber,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onCancelPlan,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = JarvisAlertRed
                                    ),
                                    modifier = Modifier.border(1.dp, JarvisAlertRed, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            } else {
                                Text(
                                    text = "Task execution completed successfully.",
                                    color = JarvisSuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "EXECUTION STEPS (${activePlan.steps.size})",
                    color = JarvisTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(activePlan.steps, key = { it.id }) { step ->
                StepCard(
                    step = step,
                    isCurrentStep = isExecuting && step.index == currentStepIndex
                )
            }
        }
    }
}

@Composable
fun StepCard(
    step: PlanStep,
    isCurrentStep: Boolean
) {
    val borderColor = when (step.status) {
        StepStatus.SUCCESS -> JarvisSuccessGreen
        StepStatus.FAILED -> JarvisAlertRed
        StepStatus.IN_PROGRESS -> JarvisCyan
        else -> JarvisSurfaceBorder
    }

    Surface(
        color = JarvisSurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Status Icon / Step Number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when (step.status) {
                            StepStatus.SUCCESS -> JarvisSuccessGreen.copy(alpha = 0.2f)
                            StepStatus.FAILED -> JarvisAlertRed.copy(alpha = 0.2f)
                            StepStatus.IN_PROGRESS -> JarvisCyan.copy(alpha = 0.2f)
                            else -> JarvisSurfaceCardHigh
                        }
                    )
                    .border(1.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.SUCCESS -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = JarvisSuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.FAILED -> Icon(
                        Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = JarvisAlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    StepStatus.IN_PROGRESS -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = JarvisCyan,
                        strokeWidth = 2.dp
                    )
                    else -> Text(
                        text = "${step.index}",
                        color = JarvisTextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        color = JarvisTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(JarvisSurfaceCardHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = step.tool,
                            color = JarvisCyanLight,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = step.description,
                    color = JarvisTextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (step.requiredPermission != null) {
                    Text(
                        text = "Permission: ${step.requiredPermission.substringAfterLast('.')}",
                        color = JarvisCyanDark,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (step.resultMessage != null) {
                    Surface(
                        color = JarvisObsidianBg,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .border(0.5.dp, borderColor, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = step.resultMessage!!,
                            color = if (step.status == StepStatus.FAILED) JarvisAlertRed else JarvisCyanLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBadge(level: RiskLevel) {
    val (color, text) = when (level) {
        RiskLevel.LOW -> Pair(JarvisSuccessGreen, "LOW RISK")
        RiskLevel.MEDIUM -> Pair(JarvisWarningAmber, "MEDIUM RISK")
        RiskLevel.HIGH -> Pair(JarvisAlertRed, "HIGH RISK")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
