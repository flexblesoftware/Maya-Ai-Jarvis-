package com.maya.android.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maya.android.app.data.PreferencesManager
import com.maya.android.app.service.MayaAccessibilityService
import com.maya.android.app.service.MayaNotificationListenerService
import com.maya.android.app.ui.theme.AmoledBackground
import com.maya.android.app.ui.theme.DarkSurface
import com.maya.android.app.ui.theme.DarkSurfaceVariant
import com.maya.android.app.ui.theme.GirlfriendPink
import com.maya.android.app.ui.theme.JarvisCyan
import com.maya.android.app.ui.theme.SuccessGreen
import com.maya.android.app.ui.theme.TextPrimary
import com.maya.android.app.ui.theme.TextSecondary
import com.maya.android.app.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    isGirlfriendMode: Boolean,
    onNavigateToVoicePicker: () -> Unit,
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = if (isGirlfriendMode) GirlfriendPink else JarvisCyan
    val scrollState = rememberScrollState()

    var userName by remember { mutableStateOf(prefs.userName) }
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var wakeWordEnabled by remember { mutableStateOf(prefs.wakeWordEnabled) }
    var wakeWordSensitivity by remember { mutableFloatStateOf(prefs.wakeWordSensitivity) }
    var askConfirmation by remember { mutableStateOf(prefs.askConfirmationBeforeAction) }
    var announceMessages by remember { mutableStateOf(prefs.announceIncomingMessages) }
    var jarvisExtra by remember { mutableStateOf(prefs.jarvisExtraInstructions) }
    var girlfriendExtra by remember { mutableStateOf(prefs.girlfriendExtraInstructions) }

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Check system permission statuses
    val isAccessibilityActive = MayaAccessibilityService.isServiceRunning()
    val isNotificationListenerActive = MayaNotificationListenerService.isServiceConnected()
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings & Configuration",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. PROFILE & GEMINI API KEY
            Text("AI Credentials & Identity", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                    prefs.userName = it
                },
                label = { Text("Your Name", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.geminiApiKey = it
                },
                label = { Text("Gemini API Key", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. VOICE & PERSONA
            Text("Voice & Personas", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToVoicePicker() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Selected Gemini Voices", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Jarvis: ${prefs.jarvisVoice} • Girlfriend: ${prefs.girlfriendVoice}",
                            color = accentColor,
                            fontSize = 13.sp
                        )
                    }
                    Icon(Icons.Default.VolumeUp, contentDescription = "Pick Voices", tint = accentColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = jarvisExtra,
                onValueChange = {
                    jarvisExtra = it
                    prefs.jarvisExtraInstructions = it
                },
                label = { Text("Jarvis Mode Extra Instructions", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = girlfriendExtra,
                onValueChange = {
                    girlfriendExtra = it
                    prefs.girlfriendExtraInstructions = it
                },
                label = { Text("Girlfriend Mode Extra Instructions", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. WAKE-WORD DETECTION
            Text("Wake-Word Engine", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Always-Listening Wake Word", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Trigger with \"Hey Maya\" or \"Hey Jarvis\" in background", color = TextTertiary, fontSize = 12.sp)
                }
                Switch(
                    checked = wakeWordEnabled,
                    onCheckedChange = {
                        wakeWordEnabled = it
                        prefs.wakeWordEnabled = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Sensitivity: ${(wakeWordSensitivity * 100).toInt()}%",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = wakeWordSensitivity,
                    onValueChange = {
                        wakeWordSensitivity = it
                        prefs.wakeWordSensitivity = it
                    },
                    valueRange = 0.2f..0.95f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. SAFETY & AGENT SETTINGS
            Text("Safety & Phone Control Guardrails", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ask Confirmation Before Calls/SMS", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Speaks confirmation and shows interactive Yes/No card", color = TextTertiary, fontSize = 12.sp)
                }
                Switch(
                    checked = askConfirmation,
                    onCheckedChange = {
                        askConfirmation = it
                        prefs.askConfirmationBeforeAction = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Announce Incoming Messages", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Reads incoming WhatsApp/Telegram texts and drafts reply", color = TextTertiary, fontSize = 12.sp)
                }
                Switch(
                    checked = announceMessages,
                    onCheckedChange = {
                        announceMessages = it
                        prefs.announceIncomingMessages = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. PERMISSIONS CHECKLIST
            Text("System Permissions Checklist", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            PermissionItem(
                title = "Accessibility Phone Agent",
                subtitle = "Required to read screen, tap buttons and automate apps",
                isGranted = isAccessibilityActive,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            PermissionItem(
                title = "Notification Listener",
                subtitle = "Required to announce incoming messages and send replies",
                isGranted = isNotificationListenerActive,
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            PermissionItem(
                title = "Display Over Other Apps (Overlay)",
                subtitle = "Required for the floating red Emergency Stop bubble",
                isGranted = canDrawOverlays,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            PermissionItem(
                title = "Ignore Battery Optimization",
                subtitle = "Ensures background wake-word listening stays alive",
                isGranted = isIgnoringBattery,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. PRIVACY & DELETE ALL DATA
            Button(
                onClick = { showDeleteAllDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3D00).copy(alpha = 0.15f),
                    contentColor = Color(0xFFFF3D00)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFF3D00).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("delete_all_data_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF3D00))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All My Data", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Data?", color = TextPrimary) },
            text = { Text("This will erase all saved memories, scheduled reminders, action audit logs, and settings from this device.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00), contentColor = Color.White)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextTertiary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Granted" else "Action Needed",
                    tint = if (isGranted) SuccessGreen else Color(0xFFFFD600),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isGranted) "Enabled" else "Tap to Enable",
                    color = if (isGranted) SuccessGreen else Color(0xFFFFD600),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
