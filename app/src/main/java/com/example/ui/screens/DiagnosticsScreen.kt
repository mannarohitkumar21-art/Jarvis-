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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CheckStatus
import com.example.model.DiagnosticCheck
import com.example.model.RepairOutcome
import com.example.ui.theme.JarvisAlertRed
import com.example.ui.theme.JarvisCyan
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
fun DiagnosticsScreen(
    checks: List<DiagnosticCheck>,
    isScanning: Boolean,
    repairOutcome: RepairOutcome?,
    repairStepProgress: Pair<Int, String>?,
    onScanClick: () -> Unit,
    onRunRepair: (procedureId: String) -> Unit,
    onDismissOutcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val healthyCount = checks.count { it.status == CheckStatus.HEALTHY }
    val warningCount = checks.count { it.status == CheckStatus.WARNING }
    val criticalCount = checks.count { it.status == CheckStatus.CRITICAL }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIAGNOSTICS & SELF-REPAIR ENGINE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Automated root-cause analysis & safe recovery",
                        color = JarvisTextDim,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onScanClick,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = JarvisObsidianBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_diagnostics_scan_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = JarvisObsidianBg,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(if (isScanning) "Scanning..." else "Full Scan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Telemetry Summary Card
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$healthyCount", color = JarvisSuccessGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "HEALTHY", color = JarvisTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$warningCount", color = JarvisWarningAmber, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "WARNING", color = JarvisTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$criticalCount", color = JarvisAlertRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "CRITICAL", color = JarvisTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Live Repair Step Progress
        if (repairStepProgress != null) {
            item {
                Surface(
                    color = JarvisNeonPurple.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisNeonPurple, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = JarvisNeonPurple,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AUTONOMOUS REPAIR [STEP ${repairStepProgress.first}/7]",
                                color = JarvisNeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = repairStepProgress.second,
                                color = JarvisTextWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Repair Outcome Dialog Card
        if (repairOutcome != null) {
            item {
                Surface(
                    color = if (repairOutcome.success) JarvisSuccessGreen.copy(alpha = 0.12f) else JarvisAlertRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (repairOutcome.success) JarvisSuccessGreen else JarvisAlertRed,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (repairOutcome.success) "SELF-REPAIR SUCCESSFUL" else "SELF-REPAIR UNRESOLVED",
                                color = if (repairOutcome.success) JarvisSuccessGreen else JarvisAlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = onDismissOutcome, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = JarvisTextDim, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = repairOutcome.message,
                            color = JarvisTextWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        repairOutcome.stepResults.forEach { stepText ->
                            Text(
                                text = "• $stepText",
                                color = JarvisTextDim,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "SUBSYSTEM HEALTH SPECIFICATION (${checks.size})",
                color = JarvisTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(checks, key = { it.id }) { check ->
            DiagnosticCard(
                check = check,
                onRunRepair = onRunRepair
            )
        }
    }
}

@Composable
fun DiagnosticCard(
    check: DiagnosticCheck,
    onRunRepair: (procedureId: String) -> Unit
) {
    val (statusColor, statusLabel) = when (check.status) {
        CheckStatus.HEALTHY -> Pair(JarvisSuccessGreen, "HEALTHY")
        CheckStatus.WARNING -> Pair(JarvisWarningAmber, "ATTENTION")
        CheckStatus.CRITICAL -> Pair(JarvisAlertRed, "CRITICAL")
        CheckStatus.NOT_AVAILABLE -> Pair(JarvisTextDim, "N/A")
    }

    Surface(
        color = JarvisSurfaceCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = check.name,
                    color = JarvisTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(0.5.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = check.summary,
                color = JarvisCyanLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = check.details,
                color = JarvisTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (check.possibleCause != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Probable Cause: ${check.possibleCause}",
                    color = JarvisWarningAmber,
                    fontSize = 11.sp
                )
            }

            if (check.repairable && check.repairProcedureId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onRunRepair(check.repairProcedureId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisNeonPurple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("repair_button_${check.id}")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("7-Step Auto Repair", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
