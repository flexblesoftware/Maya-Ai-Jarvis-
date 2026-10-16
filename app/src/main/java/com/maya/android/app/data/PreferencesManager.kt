package com.maya.android.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "maya_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w("PreferencesManager", "EncryptedSharedPreferences failed, using standard SharedPreferences fallback", e)
        context.getSharedPreferences("maya_standard_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_USER_NAME = "user_name"
        const val KEY_ACTIVE_MODE = "active_mode"
        const val KEY_JARVIS_VOICE = "jarvis_voice"
        const val KEY_GIRLFRIEND_VOICE = "girlfriend_voice"
        const val KEY_JARVIS_EXTRA = "jarvis_extra"
        const val KEY_GIRLFRIEND_EXTRA = "girlfriend_extra"
        const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        const val KEY_WAKE_WORD_MODEL = "wake_word_model"
        const val KEY_WAKE_WORD_SENSITIVITY = "wake_word_sensitivity"
        const val KEY_CONFIRM_ACTIONS = "confirm_actions"
        const val KEY_ANNOUNCE_MESSAGES = "announce_messages"
        const val KEY_SCREEN_STREAMING = "screen_streaming"
        const val KEY_IS_ONBOARDED = "is_onboarded"

        const val MODE_JARVIS = "jarvis"
        const val MODE_GIRLFRIEND = "girlfriend"
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Friend") ?: "Friend"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var activeMode: String
        get() = prefs.getString(KEY_ACTIVE_MODE, MODE_JARVIS) ?: MODE_JARVIS
        set(value) = prefs.edit().putString(KEY_ACTIVE_MODE, value).apply()

    var jarvisVoice: String
        get() = prefs.getString(KEY_JARVIS_VOICE, "Charon") ?: "Charon"
        set(value) = prefs.edit().putString(KEY_JARVIS_VOICE, value).apply()

    var girlfriendVoice: String
        get() = prefs.getString(KEY_GIRLFRIEND_VOICE, "Sulafat") ?: "Sulafat"
        set(value) = prefs.edit().putString(KEY_GIRLFRIEND_VOICE, value).apply()

    var jarvisExtraInstructions: String
        get() = prefs.getString(KEY_JARVIS_EXTRA, "") ?: ""
        set(value) = prefs.edit().putString(KEY_JARVIS_EXTRA, value).apply()

    var girlfriendExtraInstructions: String
        get() = prefs.getString(KEY_GIRLFRIEND_EXTRA, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GIRLFRIEND_EXTRA, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWordModel: String
        get() = prefs.getString(KEY_WAKE_WORD_MODEL, "hey_jarvis") ?: "hey_jarvis"
        set(value) = prefs.edit().putString(KEY_WAKE_WORD_MODEL, value).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat(KEY_WAKE_WORD_SENSITIVITY, 0.65f)
        set(value) = prefs.edit().putFloat(KEY_WAKE_WORD_SENSITIVITY, value).apply()

    var askConfirmationBeforeAction: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_ACTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_ACTIONS, value).apply()

    var announceIncomingMessages: Boolean
        get() = prefs.getBoolean(KEY_ANNOUNCE_MESSAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_ANNOUNCE_MESSAGES, value).apply()

    var screenStreamingEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_STREAMING, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_STREAMING, value).apply()

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDED, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
