package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.integrations.DeviceIntegrations
import com.example.ui.theme.JarvisAlertRed
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisNeonPurple
import com.example.ui.theme.JarvisObsidianBg
import com.example.ui.theme.JarvisSuccessGreen
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextWhite
import com.example.voice.SupportedLanguage

@Composable
fun ArcReactorHeader(
    batteryInfo: DeviceIntegrations.BatteryInfo,
    currentLanguage: SupportedLanguage,
    isListening: Boolean,
    isSpeaking: Boolean,
    soundRmsDb: Float,
    onVoiceClick: () -> Unit,
    onStopVoice: () -> Unit,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else if (isSpeaking) 900 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        color = JarvisObsidianBg,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = JarvisSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Arc Reactor Holographic Emblem
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onHeaderClick() }
                    .testTag("header_arc_reactor")
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(46.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = (size.minDimension / 2) * 0.85f

                        // Outer glowing ring
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(JarvisCyan.copy(alpha = 0.4f), Color.Transparent),
                                center = center,
                                radius = radius * pulseScale
                            ),
                            radius = radius * pulseScale
                        )

                        // Outer HUD orbit
                        drawCircle(
                            color = JarvisCyan,
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Inner dashed ring
                        drawCircle(
                            color = if (isListening) JarvisAlertRed else if (isSpeaking) JarvisNeonPurple else JarvisCyanDark,
                            radius = radius * 0.65f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Central core
                        drawCircle(
                            color = if (isListening) JarvisAlertRed else JarvisCyan,
                            radius = radius * 0.3f * if (isListening) (1f + (soundRmsDb.coerceIn(0f, 10f) / 10f)) else 1f
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JARVIS",
                            color = JarvisTextWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v2.5 AI",
                                color = JarvisCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (isListening) "VOICE LISTENING..." else if (isSpeaking) "SYNTHESIZING..." else "AUTONOMOUS CORE ONLINE",
                        color = if (isListening) JarvisAlertRed else if (isSpeaking) JarvisNeonPurple else JarvisTextDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status and Voice Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceCard)
                        .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = "Battery Status",
                        tint = if (batteryInfo.percentage < 20) JarvisAlertRed else JarvisSuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${batteryInfo.percentage}%",
                        color = JarvisTextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Language Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceCard)
                        .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentLanguage.code.uppercase(),
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Voice Mic Button
                IconButton(
                    onClick = {
                        if (isSpeaking) {
                            onStopVoice()
                        } else {
                            onVoiceClick()
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) JarvisAlertRed.copy(alpha = 0.25f)
                            else if (isSpeaking) JarvisNeonPurple.copy(alpha = 0.25f)
                            else JarvisCyan.copy(alpha = 0.15f)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isListening) JarvisAlertRed else if (isSpeaking) JarvisNeonPurple else JarvisCyan,
                            shape = CircleShape
                        )
                        .testTag("header_voice_button")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isSpeaking) "Stop Speaking" else "Voice Control",
                        tint = if (isListening) JarvisAlertRed else if (isSpeaking) JarvisNeonPurple else JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
