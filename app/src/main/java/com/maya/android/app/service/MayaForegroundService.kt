package com.maya.android.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.maya.android.app.MainActivity
import com.maya.android.app.MayaApplication
import com.maya.android.app.R
import com.maya.android.app.ai.AudioInputManager
import com.maya.android.app.ai.AudioOutputManager
import com.maya.android.app.ai.DefaultWakeWordEngine
import com.maya.android.app.ai.GeminiLiveClient
import com.maya.android.app.ai.LiveState
import com.maya.android.app.data.PreferencesManager
import com.maya.android.app.tools.MemoryToolExecutor
import com.maya.android.app.tools.PendingActionConfirmation
import com.maya.android.app.tools.PhoneToolExecutor
import com.maya.android.app.tools.ReminderToolExecutor
import com.maya.android.app.tools.ScreenAgentExecutor
import com.maya.android.app.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MayaForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.maya.android.app.ACTION_START"
        const val ACTION_STOP = "com.maya.android.app.ACTION_STOP"
        const val ACTION_TALK = "com.maya.android.app.ACTION_TALK"
        const val ACTION_PAUSE_RESUME = "com.maya.android.app.ACTION_PAUSE_RESUME"
        const val NOTIFICATION_ID = 1001

        var instance: MayaForegroundService? = null
            private set

        fun startService(context: Context) {
            val intent = Intent(context, MayaForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MayaForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: MayaForegroundService get() = this@MayaForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private lateinit var prefs: PreferencesManager
    private lateinit var wakeWordEngine: DefaultWakeWordEngine
    private lateinit var audioInputManager: AudioInputManager
    private lateinit var audioOutputManager: AudioOutputManager
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var geminiClient: GeminiLiveClient
    private var overlayBubbleManager: OverlayBubbleManager? = null

    private var isListeningPaused = false

    // Reactive State flows for UI binding
    val liveStateFlow = MutableStateFlow(LiveState.IDLE)
    val userTranscriptFlow = MutableStateFlow("")
    val mayaTranscriptFlow = MutableStateFlow("")
    val statusMessageFlow = MutableStateFlow("Maya is ready.")
    val pendingConfirmationFlow = MutableStateFlow<PendingActionConfirmation?>(null)

    override fun onCreate() {
        super.onCreate()
        instance = this
        val app = application as MayaApplication
        prefs = app.preferencesManager

        wakeWordEngine = DefaultWakeWordEngine().apply {
            setSensitivity(prefs.wakeWordSensitivity)
            setModel(prefs.wakeWordModel)
            initialize(this@MayaForegroundService)
        }

        audioOutputManager = AudioOutputManager(this)

        val phoneExecutor = PhoneToolExecutor(this, prefs) { confirmation ->
            pendingConfirmationFlow.value = confirmation
        }
        val screenExecutor = ScreenAgentExecutor(this)
        val memoryExecutor = MemoryToolExecutor(app.database.memoryDao())
        val reminderExecutor = ReminderToolExecutor(this, app.database.reminderDao())

        toolRegistry = ToolRegistry(
            this,
            phoneExecutor,
            screenExecutor,
            memoryExecutor,
            reminderExecutor
        )

        geminiClient = GeminiLiveClient(
            preferencesManager = prefs,
            toolRegistry = toolRegistry,
            audioOutputManager = audioOutputManager,
            onStateChanged = { state ->
                liveStateFlow.value = state
                if (state == LiveState.IDLE) {
                    audioInputManager.setLiveSessionActive(false)
                    overlayBubbleManager?.hideStopBubble()
                } else if (state == LiveState.LISTENING || state == LiveState.SPEAKING) {
                    audioInputManager.setLiveSessionActive(true)
                    overlayBubbleManager?.showStopBubble()
                }
            },
            onUserTranscript = { text ->
                userTranscriptFlow.value = text
            },
            onMayaTranscript = { text ->
                mayaTranscriptFlow.value += text
            },
            onError = { err ->
                statusMessageFlow.value = err
            },
            onSessionTimeout = {
                audioInputManager.setLiveSessionActive(false)
                statusMessageFlow.value = "Session ended. Wake-word active."
            }
        )

        audioInputManager = AudioInputManager(
            context = this,
            wakeWordEngine = wakeWordEngine,
            onWakeWordDetected = {
                handleWakeWordTriggered()
            },
            onLiveAudioChunk = { pcmChunk ->
                geminiClient.sendAudioChunk(pcmChunk)
            }
        )

        overlayBubbleManager = OverlayBubbleManager(this) {
            abortAllAgentTasks()
        }

        // Setup notification message listener
        MayaNotificationListenerService.messageListener = { event ->
            if (prefs.announceIncomingMessages) {
                handleIncomingMessage(event)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TALK -> {
                startLiveSession()
            }
            ACTION_PAUSE_RESUME -> {
                isListeningPaused = !isListeningPaused
                if (isListeningPaused) {
                    audioInputManager.stop()
                    statusMessageFlow.value = "Listening paused"
                } else {
                    audioInputManager.start(serviceScope)
                    statusMessageFlow.value = "Listening active"
                }
                updateNotification()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                if (prefs.wakeWordEnabled && !isListeningPaused) {
                    audioInputManager.start(serviceScope)
                }
            }
        }
        return START_STICKY
    }

    private fun handleWakeWordTriggered() {
        Log.i("MayaForegroundService", "Wake-word triggered! Waking screen and launching Live Session.")
        playChime()
        wakeScreenBriefly()

        // Open MainActivity over lock screen
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_START_LISTENING", true)
        }
        startActivity(openIntent)

        startLiveSession()
    }

    fun startLiveSession() {
        audioInputManager.setLiveSessionActive(true)
        mayaTranscriptFlow.value = ""
        serviceScope.launch(Dispatchers.IO) {
            val memories = (application as MayaApplication).database.memoryDao().getAllMemories()
            val memorySummary = memories.take(6).joinToString("\n") { "- ${it.fact}" }
            geminiClient.startSession(serviceScope, memoryContext = memorySummary)
        }
    }

    fun queryPrompt(text: String) {
        userTranscriptFlow.value = text
        mayaTranscriptFlow.value = ""
        serviceScope.launch(Dispatchers.IO) {
            val memories = (application as MayaApplication).database.memoryDao().getAllMemories()
            val memorySummary = memories.take(6).joinToString("\n") { "- ${it.fact}" }
            geminiClient.queryGeminiRest(text, memorySummary)
        }
    }

    fun speakDirectly(text: String) {
        val isJarvis = prefs.activeMode == PreferencesManager.MODE_JARVIS
        val voice = com.maya.android.app.ai.VoiceDirectory.findVoice(
            if (isJarvis) prefs.jarvisVoice else prefs.girlfriendVoice
        )
        audioOutputManager.speakText(text, voice.pitch, voice.speechRate)
    }

    fun stopLiveSession() {
        geminiClient.disconnect()
        audioInputManager.setLiveSessionActive(false)
        overlayBubbleManager?.hideStopBubble()
    }

    fun abortAllAgentTasks() {
        toolRegistry.screenExecutor.abortCurrentTask()
        audioOutputManager.flush()
        stopLiveSession()
        statusMessageFlow.value = "Cancelled all agent actions."
    }

    private fun playChime() {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, alert)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (e: Exception) {
            Log.w("MayaForegroundService", "Error playing chime", e)
        }
    }

    private fun wakeScreenBriefly() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Maya:WakeLock"
            )
            wakeLock.acquire(4000L)
        } catch (e: Exception) {
            Log.w("MayaForegroundService", "Could not wake screen", e)
        }
    }

    private fun handleIncomingMessage(event: IncomingMessageEvent) {
        serviceScope.launch(Dispatchers.Main) {
            statusMessageFlow.value = "Message from ${event.sender}: ${event.text}"
            // Prompt user with spoken card
            pendingConfirmationFlow.value = PendingActionConfirmation(
                actionType = "message_reply",
                description = "New message from ${event.sender}",
                details = "\"${event.text}\"",
                onConfirm = {
                    if (event.replyAction != null) {
                        val reply = "Got it! Thanks."
                        val sent = MayaNotificationListenerService.instance?.replyToNotification(event.replyAction, reply) ?: false
                        if (sent) "Sent reply: \"$reply\"" else "Could not send reply."
                    } else {
                        "No direct reply option available."
                    }
                },
                onCancel = { "Message dismissed." }
            )
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val talkIntent = Intent(this, MayaForegroundService::class.java).apply { action = ACTION_TALK }
        val pendingTalk = PendingIntent.getService(
            this,
            1,
            talkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, MayaForegroundService::class.java).apply { action = ACTION_PAUSE_RESUME }
        val pendingPauseResume = PendingIntent.getService(
            this,
            2,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeLabel = if (isListeningPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, MayaApplication.CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Maya Voice Assistant")
            .setContentText(if (isListeningPaused) "Listening paused" else "Listening for wake word...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_launcher_foreground, "Talk", pendingTalk)
            .addAction(R.drawable.ic_launcher_foreground, pauseResumeLabel, pendingPauseResume)
            .build()
    }

    private fun updateNotification() {
        val notification = buildForegroundNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        audioInputManager.stop()
        audioOutputManager.release()
        geminiClient.disconnect()
        wakeWordEngine.release()
        overlayBubbleManager?.hideStopBubble()
        serviceScope.cancel()
        Log.i("MayaForegroundService", "Service destroyed")
    }
}
