package com.maya.android.app.tools

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.maya.android.app.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class PendingActionConfirmation(
    val actionType: String, // e.g. "call", "sms", "delete"
    val description: String,
    val details: String,
    val onConfirm: suspend () -> String,
    val onCancel: () -> String
)

class PhoneToolExecutor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val requestConfirmation: (PendingActionConfirmation) -> Unit
) {

    // Sensitive keyword blocklist (Safety Requirement)
    private val blockedKeywords = listOf(
        "bank", "upi", "paytm", "gpay", "phonepe", "otp", "pin", "cvv", "password", "transfer money", "credit card", "debit card"
    )

    fun isRestrictedSafetyKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return blockedKeywords.any { lower.contains(it) }
    }

    suspend fun openApp(appName: String): String = withContext(Dispatchers.Default) {
        if (isRestrictedSafetyKeyword(appName)) {
            return@withContext "Safety rule: Operating banking, payment, or credential-sensitive applications is strictly prohibited."
        }

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Find package by matching app label or package name
        var matchedPackage: String? = null
        for (app in packages) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.equals(appName, ignoreCase = true) || label.contains(appName, ignoreCase = true)) {
                matchedPackage = app.packageName
                break
            }
        }

        if (matchedPackage == null) {
            // Check direct package match
            if (appName.contains(".") && packages.any { it.packageName.equals(appName, ignoreCase = true) }) {
                matchedPackage = appName
            }
        }

        if (matchedPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return@withContext "Opened $appName."
            }
        }

        "App \"$appName\" could not be found or opened."
    }

    suspend fun searchContacts(query: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && results.size < 5) {
                    val name = it.getString(nameIdx) ?: "Unknown"
                    val num = it.getString(numIdx) ?: ""
                    results.add(Pair(name, num))
                }
            }
        } catch (e: Exception) {
            Log.w("PhoneToolExecutor", "Contact search failed", e)
        }
        results
    }

    suspend fun callContact(contactOrNumber: String): String {
        var targetNumber = contactOrNumber.trim()
        var targetName = contactOrNumber.trim()

        // If not numeric, look up in contacts
        if (!targetNumber.matches(Regex("^[+0-9\\s\\-()]+$"))) {
            val contacts = searchContacts(contactOrNumber)
            if (contacts.isEmpty()) {
                return "Could not find any contact named \"$contactOrNumber\"."
            }
            targetName = contacts[0].first
            targetNumber = contacts[0].second
        }

        val executeCall = suspend {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$targetNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Calling $targetName ($targetNumber)..."
            } catch (e: Exception) {
                // Fallback to DIAL intent if CALL_PHONE permission not yet granted
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$targetNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                "Opened dialer for $targetName ($targetNumber)."
            }
        }

        if (preferencesManager.askConfirmationBeforeAction) {
            return withContext(Dispatchers.Main) {
                requestConfirmation(
                    PendingActionConfirmation(
                        actionType = "call",
                        description = "Call $targetName",
                        details = "Number: $targetNumber",
                        onConfirm = executeCall,
                        onCancel = { "Call to $targetName was cancelled." }
                    )
                )
                "I need your confirmation before placing a call to $targetName. Please confirm on screen or say Yes."
            }
        } else {
            return executeCall()
        }
    }

    suspend fun sendSms(contactOrNumber: String, message: String): String {
        var targetNumber = contactOrNumber.trim()
        var targetName = contactOrNumber.trim()

        if (isRestrictedSafetyKeyword(message)) {
            return "Safety rule: Cannot transmit OTPs, passwords, or banking credentials via SMS."
        }

        if (!targetNumber.matches(Regex("^[+0-9\\s\\-()]+$"))) {
            val contacts = searchContacts(contactOrNumber)
            if (contacts.isEmpty()) {
                return "Could not find any contact named \"$contactOrNumber\"."
            }
            targetName = contacts[0].first
            targetNumber = contacts[0].second
        }

        val executeSend = suspend {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                smsManager.sendTextMessage(targetNumber, null, message, null, null)
                "SMS sent to $targetName ($targetNumber): \"$message\""
            } catch (e: Exception) {
                // Fallback to SMS app intent
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:$targetNumber")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Opened messaging composer for $targetName."
            }
        }

        if (preferencesManager.askConfirmationBeforeAction) {
            return withContext(Dispatchers.Main) {
                requestConfirmation(
                    PendingActionConfirmation(
                        actionType = "sms",
                        description = "Send SMS to $targetName",
                        details = "\"$message\"",
                        onConfirm = executeSend,
                        onCancel = { "Sending SMS to $targetName was cancelled." }
                    )
                )
                "Should I send this SMS to $targetName: \"$message\"? Please confirm."
            }
        } else {
            return executeSend()
        }
    }

    fun openUrl(url: String): String {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opening $validUrl"
        } catch (e: Exception) {
            "Could not open link: ${e.message}"
        }
    }

    fun mediaControl(action: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio service unavailable."
        val keycode = when (action.lowercase()) {
            "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keycode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
        return "Media command '$action' sent."
    }

    fun setVolume(percent: Int): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio service unavailable."
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) / 100f) * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Media volume set to $percent%."
    }

    fun setFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return "No flashlight available on device."
            cameraManager.setTorchMode(cameraId, turnOn)
            if (turnOn) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: Exception) {
            "Flashlight error: ${e.message}"
        }
    }

    fun openSettingsPanel(panel: String): String {
        return try {
            val action = when (panel.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
            "Opened $panel settings."
        } catch (e: Exception) {
            "Could not open settings panel: ${e.message}"
        }
    }

    fun getClipboard(): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString() ?: "Clipboard is empty."
            } else {
                "Clipboard is empty."
            }
        } catch (e: Exception) {
            "Could not read clipboard: ${e.message}"
        }
    }

    fun setClipboard(text: String): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Maya Clipboard", text)
            clipboard?.setPrimaryClip(clip)
            "Copied text to clipboard."
        } catch (e: Exception) {
            "Could not copy to clipboard: ${e.message}"
        }
    }

    suspend fun waitSeconds(seconds: Int): String {
        val s = seconds.coerceIn(1, 30)
        delay(s * 1000L)
        return "Waited for $s seconds."
    }
}
