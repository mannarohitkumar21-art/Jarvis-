package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audit.AuditLogger
import com.example.model.AiConfig
import com.example.model.AiProviderType
import com.example.model.AuditEntry
import com.example.model.AuditSeverity
import com.example.setup.SetupReport
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
import com.example.voice.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAuditScreen(
    aiConfig: AiConfig,
    currentLanguage: SupportedLanguage,
    auditLogs: List<AuditEntry>,
    setupReport: SetupReport?,
    onSaveAiConfig: (AiConfig) -> Unit,
    onLanguageChange: (SupportedLanguage) -> Unit,
    onTriggerSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf(aiConfig.selectedProvider) }
    var selectedModel by remember { mutableStateOf(aiConfig.selectedModel) }
    var apiKeyText by remember { mutableStateOf(aiConfig.customApiKey) }
    var customEndpoint by remember { mutableStateOf(aiConfig.customEndpoint) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "AI PROVIDER & PRIVACY SETTINGS",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Configure multi-provider models, secrets, and inspect security audit trail",
                color = JarvisTextDim,
                fontSize = 11.sp
            )
        }

        if (saveFeedback != null) {
            item {
                Surface(
                    color = JarvisSuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisSuccessGreen, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(text = saveFeedback!!, color = JarvisSuccessGreen, fontSize = 12.sp)
                }
            }
        }

        // 1. AI Provider Card
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Intelligence Engine Provider", color = JarvisTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Provider Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isProviderDropdownExpanded,
                        onExpandedChange = { isProviderDropdownExpanded = !isProviderDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProvider.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Provider", color = JarvisTextDim, fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                .testTag("provider_dropdown"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = JarvisTextWhite,
                                unfocusedTextColor = JarvisTextWhite,
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisSurfaceBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isProviderDropdownExpanded,
                            onDismissRequest = { isProviderDropdownExpanded = false }
                        ) {
                            AiProviderType.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(provider.displayName, color = JarvisTextWhite, fontWeight = FontWeight.Bold)
                                            Text(if (provider.isLocal) "Offline On-Device Core" else "Cloud Hosted", color = JarvisTextDim, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedProvider = provider
                                        selectedModel = provider.defaultModel
                                        isProviderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        label = { Text("Model Name / Identifier", color = JarvisTextDim, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("model_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JarvisTextWhite,
                            unfocusedTextColor = JarvisTextWhite,
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisSurfaceBorder
                        )
                    )

                    if (!selectedProvider.isLocal) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { apiKeyText = it },
                            label = { Text("API Key (Stored securely)", color = JarvisTextDim, fontSize = 11.sp) },
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Key Visibility",
                                        tint = JarvisTextDim
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = JarvisTextWhite,
                                unfocusedTextColor = JarvisTextWhite,
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisSurfaceBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                val updated = AiConfig(
                                    selectedProvider = selectedProvider,
                                    selectedModel = selectedModel,
                                    customApiKey = apiKeyText,
                                    customEndpoint = customEndpoint
                                )
                                onSaveAiConfig(updated)
                                saveFeedback = "Configuration successfully saved."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisObsidianBg),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_ai_config_button")
                        ) {
                            Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Multilingual Setup
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = JarvisNeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Primary Assistant Language", color = JarvisTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SupportedLanguage.values().forEach { lang ->
                        val isSelected = currentLanguage == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisNeonPurple.copy(alpha = 0.2f) else JarvisSurfaceCardHigh)
                                .border(1.dp, if (isSelected) JarvisNeonPurple else JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                                .clickable { onLanguageChange(lang) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) JarvisTextWhite else JarvisTextDim,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(JarvisNeonPurple)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Autonomous Setup Report Inspection
        item {
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
                            text = "AUTONOMOUS SETUP ENGINE",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedButton(
                            onClick = onTriggerSetup,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).border(1.dp, JarvisCyan, RoundedCornerShape(6.dp))
                        ) {
                            Text("Re-run Setup", fontSize = 10.sp, color = JarvisCyan)
                        }
                    }
                    if (setupReport != null) {
                        Text(
                            text = "Device: ${setupReport.deviceModel} (Android ${setupReport.androidVersion})",
                            color = JarvisTextWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = setupReport.summary,
                            color = JarvisTextDim,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // 4. Security & Operation Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = JarvisSuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURITY AUDIT LOGS (${auditLogs.size})",
                        color = JarvisSuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(
                    onClick = { AuditLogger.clear() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = JarvisTextDim, modifier = Modifier.size(16.dp))
                }
            }
        }

        items(auditLogs.take(30), key = { it.id }) { log ->
            AuditRow(log = log)
        }
    }
}

@Composable
fun AuditRow(log: AuditEntry) {
    val severityColor = when (log.severity) {
        AuditSeverity.INFO -> JarvisCyan
        AuditSeverity.WARNING -> JarvisWarningAmber
        AuditSeverity.ALERT -> JarvisAlertRed
        AuditSeverity.SECURITY -> JarvisNeonPurple
    }

    Surface(
        color = JarvisSurfaceCard,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${log.formattedTime}]",
                        color = JarvisTextDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.category.name,
                        color = severityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = log.outcome,
                        color = severityColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = log.action,
                color = JarvisTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = log.details,
                color = JarvisTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
