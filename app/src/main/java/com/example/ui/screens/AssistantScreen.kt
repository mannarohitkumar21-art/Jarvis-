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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.TaskPlan
import com.example.ui.theme.JarvisAlertRed
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisNeonPurple
import com.example.ui.theme.JarvisObsidianBg
import com.example.ui.theme.JarvisSurfaceBorder
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceCardHigh
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextWhite
import com.example.voice.SupportedLanguage

@Composable
fun AssistantScreen(
    messages: List<ChatMessage>,
    isListening: Boolean,
    isSpeaking: Boolean,
    voiceError: String?,
    currentLanguage: SupportedLanguage,
    onSendMessage: (String) -> Unit,
    onVoiceToggle: () -> Unit,
    onLanguageChange: (SupportedLanguage) -> Unit,
    onOpenPlan: (TaskPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianBg)
    ) {
        // Multilingual Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOICE / LANGUAGE CONTEXT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SupportedLanguage.values().forEach { lang ->
                    val isSelected = currentLanguage == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.2f) else JarvisSurfaceCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) JarvisCyan else JarvisSurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onLanguageChange(lang) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lang.code.uppercase(),
                            color = if (isSelected) JarvisCyan else JarvisTextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Voice error banner if any
        if (voiceError != null) {
            Surface(
                color = JarvisAlertRed.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, JarvisAlertRed, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Voice Engine: $voiceError",
                    color = JarvisAlertRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    onOpenPlan = onOpenPlan
                )
            }
        }

        // Quick Suggestion Chips
        val suggestions = when (currentLanguage) {
            SupportedLanguage.BENGALI -> listOf(
                "প্ল্যান: ব্যাটারি চেক ও টর্চ অন",
                "সিস্টেম ডায়াগনস্টিক চালাও",
                "নমস্কার জারভিস",
                "স্টোরেজ স্পেস দেখাও"
            )
            SupportedLanguage.HINDI -> listOf(
                "प्लान: बैटरी जांच और टॉर्च ऑन",
                "सिस्टम डायग्नोस्टिक्स चलाएं",
                "नमस्ते जार्विस",
                "स्टोरेज स्पेस दिखाएं"
            )
            SupportedLanguage.ENGLISH -> listOf(
                "Plan: Check battery and turn on torch",
                "Plan: Schedule 7 AM wake up alarm",
                "Run system diagnostics",
                "Launch settings app",
                "What can you do?"
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JarvisSurfaceCard)
                        .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(16.dp))
                        .clickable { onSendMessage(chip) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chip,
                        color = JarvisCyanLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Input Bar
        Surface(
            color = JarvisSurfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onVoiceToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) JarvisAlertRed.copy(alpha = 0.2f) else JarvisSurfaceCardHigh)
                        .border(1.dp, if (isListening) JarvisAlertRed else JarvisSurfaceBorder, CircleShape)
                        .testTag("chat_voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) JarvisAlertRed else JarvisCyan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to voice instruction..." else "Ask JARVIS or give a plan instruction...",
                            color = JarvisTextDim,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextWhite,
                        unfocusedTextColor = JarvisTextWhite,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceBorder,
                        focusedContainerColor = JarvisObsidianBg,
                        unfocusedContainerColor = JarvisObsidianBg
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(JarvisCyan)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = JarvisObsidianBg
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onOpenPlan: (TaskPlan) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.15f))
                    .border(1.dp, JarvisCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "JARVIS",
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) JarvisCyan.copy(alpha = 0.15f) else JarvisSurfaceCard,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (isUser) JarvisCyan else JarvisSurfaceBorder,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        color = JarvisTextWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Associated Plan Card Preview
                    if (message.associatedPlan != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = JarvisSurfaceCardHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, JarvisNeonPurple, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TASK PLAN: ${message.associatedPlan.steps.size} STEPS",
                                        color = JarvisNeonPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    ElevatedButton(
                                        onClick = { onOpenPlan(message.associatedPlan) },
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = JarvisNeonPurple,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Open Planner", fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = message.associatedPlan.intent,
                                    color = JarvisTextWhite,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (message.executionCommand != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$ ${message.executionCommand}",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
