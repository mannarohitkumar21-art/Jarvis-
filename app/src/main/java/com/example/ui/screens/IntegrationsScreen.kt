package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integrations.DeviceIntegrations
import com.example.permission.JarvisPermissionManager
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
fun IntegrationsScreen(
    deviceIntegrations: DeviceIntegrations,
    permissionManager: JarvisPermissionManager,
    modifier: Modifier = Modifier
) {
    var batteryInfo by remember { mutableStateOf(deviceIntegrations.getBatteryInfo()) }
    var isTorchOn by remember { mutableStateOf(deviceIntegrations.isFlashlightActive()) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    // App launcher state
    var appQuery by remember { mutableStateOf("") }

    // Alarm scheduler state
    var alarmHour by remember { mutableStateOf("7") }
    var alarmMinute by remember { mutableStateOf("0") }
    var alarmMessage by remember { mutableStateOf("JARVIS Wake Up") }

    // Location state
    var locationDisplay by remember { mutableStateOf<String?>(null) }

    // Photo picker state (Zero permission modern API)
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedMediaUri = uri
        if (uri != null) {
            actionFeedback = "User selected photo: ${uri.lastPathSegment}"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "PERMISSION-AWARE DEVICE INTEGRATIONS",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Official Android API handlers with explicit security policies.",
                color = JarvisTextDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (actionFeedback != null) {
            item {
                Surface(
                    color = JarvisCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = actionFeedback!!,
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 1. Battery Telemetry
        item {
            IntegrationCard(
                icon = Icons.Default.BatteryFull,
                iconColor = if (batteryInfo.percentage < 20) JarvisAlertRed else JarvisSuccessGreen,
                title = "Battery & Power Subsystem",
                subtitle = "Hardware state & thermal monitoring"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Charge Level", color = JarvisTextDim, fontSize = 11.sp)
                        Text(text = "${batteryInfo.percentage}%", color = JarvisTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "Status", color = JarvisTextDim, fontSize = 11.sp)
                        Text(text = batteryInfo.chargingType, color = JarvisTextWhite, fontSize = 14.sp)
                    }
                    Column {
                        Text(text = "Temperature", color = JarvisTextDim, fontSize = 11.sp)
                        Text(text = "${batteryInfo.temperatureCelsius}°C", color = JarvisTextWhite, fontSize = 14.sp)
                    }
                    Column {
                        Text(text = "Health", color = JarvisTextDim, fontSize = 11.sp)
                        Text(text = batteryInfo.health, color = JarvisSuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Hardware Flashlight
        item {
            IntegrationCard(
                icon = Icons.Default.FlashlightOn,
                iconColor = if (isTorchOn) JarvisCyan else JarvisTextDim,
                title = "Hardware Flashlight / Torch",
                subtitle = "Requires CAMERA permission for LED strobe"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTorchOn) "Flashlight is ACTIVE" else "Flashlight is OFF",
                        color = if (isTorchOn) JarvisCyan else JarvisTextDim,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = isTorchOn,
                        onCheckedChange = { checked ->
                            val res = deviceIntegrations.toggleTorch(checked)
                            if (res.isSuccess) {
                                isTorchOn = res.getOrNull() == true
                                actionFeedback = "Flashlight toggled ${if (isTorchOn) "ON" else "OFF"}"
                            } else {
                                actionFeedback = "Torch error: ${res.exceptionOrNull()?.message}"
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyan,
                            checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = JarvisTextDim,
                            uncheckedTrackColor = JarvisSurfaceCardHigh
                        ),
                        modifier = Modifier.testTag("torch_switch")
                    )
                }
            }
        }

        // 3. Application Launcher
        item {
            IntegrationCard(
                icon = Icons.Default.Launch,
                iconColor = JarvisCyanDark,
                title = "Application Launcher",
                subtitle = "Query PackageManager & launch via explicit intent"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        placeholder = { Text("App name (e.g., Settings, Camera, Maps)", color = JarvisTextDim, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("app_launch_input"),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (appQuery.isNotBlank()) {
                                val res = deviceIntegrations.launchApp(appQuery)
                                actionFeedback = if (res.isSuccess) res.getOrNull() else "Error: ${res.exceptionOrNull()?.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisObsidianBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("app_launch_button")
                    ) {
                        Text("Launch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Android Clock / Alarm Intents
        item {
            IntegrationCard(
                icon = Icons.Default.Alarm,
                iconColor = JarvisWarningAmber,
                title = "Alarm & Timer Scheduler",
                subtitle = "Dispatches AlarmClock.ACTION_SET_ALARM"
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alarmHour,
                        onValueChange = { alarmHour = it },
                        label = { Text("Hour (0-23)", fontSize = 10.sp, color = JarvisTextDim) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = JarvisTextWhite, unfocusedTextColor = JarvisTextWhite)
                    )
                    OutlinedTextField(
                        value = alarmMinute,
                        onValueChange = { alarmMinute = it },
                        label = { Text("Minute (0-59)", fontSize = 10.sp, color = JarvisTextDim) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = JarvisTextWhite, unfocusedTextColor = JarvisTextWhite)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = alarmMessage,
                        onValueChange = { alarmMessage = it },
                        label = { Text("Alarm Note", fontSize = 10.sp, color = JarvisTextDim) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = JarvisTextWhite, unfocusedTextColor = JarvisTextWhite)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val h = alarmHour.toIntOrNull() ?: 7
                            val m = alarmMinute.toIntOrNull() ?: 0
                            val res = deviceIntegrations.setAlarm(h, m, alarmMessage)
                            actionFeedback = if (res.isSuccess) res.getOrNull() else "Error: ${res.exceptionOrNull()?.message}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisWarningAmber, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("set_alarm_button")
                    ) {
                        Text("Set Alarm", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. GPS & Location Services
        item {
            IntegrationCard(
                icon = Icons.Default.LocationOn,
                iconColor = JarvisCyan,
                title = "Location Services",
                subtitle = "Requires ACCESS_FINE_LOCATION permission"
            ) {
                if (locationDisplay != null) {
                    Text(
                        text = locationDisplay!!,
                        color = JarvisCyanLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        val res = deviceIntegrations.getLastKnownLocation()
                        if (res.isSuccess) {
                            val loc = res.getOrNull()!!
                            locationDisplay = "Lat: ${loc.latitude}, Lon: ${loc.longitude} (±${loc.accuracyMeters}m via ${loc.provider})"
                            actionFeedback = "Location retrieved successfully"
                        } else {
                            actionFeedback = "Location failed: ${res.exceptionOrNull()?.message}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceCardHigh, contentColor = JarvisCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, JarvisCyan, RoundedCornerShape(8.dp)).testTag("query_location_button")
                ) {
                    Text("Query Coordinates", fontSize = 12.sp)
                }
            }
        }

        // 6. Camera & System Notifications
        item {
            IntegrationCard(
                icon = Icons.Default.Notifications,
                iconColor = JarvisNeonPurple,
                title = "Alerts & Camera Capture",
                subtitle = "Post system notification or trigger camera"
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val res = deviceIntegrations.postNotification(
                                title = "JARVIS Alert",
                                message = "Background autonomous diagnostics completed."
                            )
                            actionFeedback = if (res.isSuccess) "Alert notification posted" else "Failed: ${res.exceptionOrNull()?.message}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisNeonPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("post_notification_button")
                    ) {
                        Text("Post Notification", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val res = deviceIntegrations.triggerCameraCapture()
                            actionFeedback = if (res.isSuccess) res.getOrNull() else "Error: ${res.exceptionOrNull()?.message}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceCardHigh, contentColor = JarvisTextWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp)).testTag("trigger_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture", fontSize = 11.sp)
                    }
                }
            }
        }

        // 7. Zero-Permission Photo / Document Picker
        item {
            IntegrationCard(
                icon = Icons.Default.Folder,
                iconColor = JarvisCyanLight,
                title = "User Selected Media (Photo Picker)",
                subtitle = "Play Store compliant zero-permission media selector"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedMediaUri != null) "File: ${selectedMediaUri?.lastPathSegment?.take(18)}..." else "No media selected",
                        color = JarvisTextDim,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceCardHigh, contentColor = JarvisCyanLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp)).testTag("pick_photo_button")
                    ) {
                        Text("Select Media", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = JarvisSurfaceCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f))
                        .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, color = JarvisTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = JarvisTextDim, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
