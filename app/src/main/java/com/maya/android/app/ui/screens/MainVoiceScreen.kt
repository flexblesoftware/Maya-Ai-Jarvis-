package com.maya.android.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.android.app.ai.LiveState
import com.maya.android.app.tools.PendingActionConfirmation
import com.maya.android.app.ui.components.ActionConfirmationCard
import com.maya.android.app.ui.components.DynamicVoiceOrb
import com.maya.android.app.ui.theme.AmoledBackground
import com.maya.android.app.ui.theme.DarkSurface
import com.maya.android.app.ui.theme.DarkSurfaceVariant
import com.maya.android.app.ui.theme.GirlfriendPink
import com.maya.android.app.ui.theme.JarvisCyan
import com.maya.android.app.ui.theme.TextPrimary
import com.maya.android.app.ui.theme.TextSecondary
import com.maya.android.app.ui.theme.TextTertiary

@Composable
fun MainVoiceScreen(
    liveState: LiveState,
    isGirlfriendMode: Boolean,
    statusMessage: String,
    userTranscript: String,
    mayaTranscript: String,
    pendingConfirmation: PendingActionConfirmation?,
    onToggleMode: () -> Unit,
    onMicClick: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onTestVoice: () -> Unit,
    onStopAllActions: () -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val accentColor = if (isGirlfriendMode) GirlfriendPink else JarvisCyan
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val quickChipsScroll = rememberScrollState()

    LaunchedEffect(mayaTranscript) {
        if (mayaTranscript.isNotBlank()) {
            listState.animateScrollToItem(0)
        }
    }

    val quickQuestions = listOf(
        "Kaise ho Maya?",
        "What can you do?",
        "Remind me in 10 minutes",
        "Turn on flashlight",
        "Tell me a fun fact",
        "Switch to ${if (isGirlfriendMode) "Jarvis" else "Girlfriend"} mode"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER & NAVIGATION
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MAYA",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Mode Pill
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isGirlfriendMode) "GIRLFRIEND" else "JARVIS",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        if (isGirlfriendMode) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "♥ AI Companion",
                                color = GirlfriendPink.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Navigation Actions
                    Row {
                        IconButton(
                            onClick = onNavigateToMemories,
                            modifier = Modifier.testTag("nav_memories")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Memories", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigateToReminders,
                            modifier = Modifier.testTag("nav_reminders")
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = "Reminders", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.testTag("nav_logs")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Action Logs", tint = TextSecondary)
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("nav_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                        }
                    }
                }
            }

            // 2. CENTER SECTION: DYNAMIC VOICE ORB & STATUS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DynamicVoiceOrb(
                    state = liveState,
                    isGirlfriendMode = isGirlfriendMode,
                    onClick = onMicClick,
                    modifier = Modifier.testTag("voice_orb")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // State indicator text
                val stateText = when (liveState) {
                    LiveState.IDLE -> "Say \"Hey Maya\" or tap mic to speak"
                    LiveState.CONNECTING -> "Connecting to Gemini Live..."
                    LiveState.LISTENING -> "Listening to you... Boliye!"
                    LiveState.THINKING -> "Thinking..."
                    LiveState.SPEAKING -> "Maya bol rahi hai..."
                    LiveState.ERROR -> "Session error - tap mic to reconnect"
                }

                Text(
                    text = stateText,
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                if (statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMessage,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                // Action Confirmation Card
                ActionConfirmationCard(
                    confirmation = pendingConfirmation,
                    accentColor = accentColor,
                    onConfirm = onConfirmAction,
                    onCancel = onCancelAction,
                    modifier = Modifier.testTag("confirmation_card")
                )

                // Live Transcript Box (if active)
                if (mayaTranscript.isNotBlank() || userTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(DarkSurfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (userTranscript.isNotBlank()) {
                                item {
                                    Text(
                                        text = "You: $userTranscript",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            if (mayaTranscript.isNotBlank()) {
                                item {
                                    Text(
                                        text = "Maya: $mayaTranscript",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. QUICK SUGGESTION CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(quickChipsScroll)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickQuestions.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable {
                                onSendPrompt(prompt)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. DIRECT QUERY / CHAT INPUT BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask or command Maya in Hinglish...", color = TextTertiary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val text = inputText.trim()
                                    if (text.isNotBlank()) {
                                        onSendPrompt(text)
                                        inputText = ""
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = accentColor
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Test Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurfaceVariant, CircleShape)
                        .clickable { onTestVoice() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Test Voice",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 5. BOTTOM CONTROL BUTTONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle Button
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onToggleMode() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("mode_toggle_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGirlfriendMode) Icons.Default.Favorite else Icons.Default.Bolt,
                            contentDescription = "Switch Mode",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGirlfriendMode) "Jarvis Mode" else "Girlfriend Mode",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Central Glowing Mic Action Button
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = accentColor,
                    contentColor = AmoledBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("main_mic_button")
                ) {
                    Icon(
                        imageVector = if (liveState == LiveState.IDLE) Icons.Default.Mic else Icons.Default.Stop,
                        contentDescription = "Talk to Maya",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Emergency Abort Panic Button
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFFF3D00).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onStopAllActions() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("emergency_stop_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop All Actions",
                            tint = Color(0xFFFF3D00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ruko / Stop",
                            color = Color(0xFFFF3D00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
