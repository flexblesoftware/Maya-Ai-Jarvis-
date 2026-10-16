package com.maya.android.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.maya.android.app.ai.LiveState
import com.maya.android.app.data.PreferencesManager
import com.maya.android.app.data.model.ActionLogEntity
import com.maya.android.app.data.model.MemoryEntity
import com.maya.android.app.data.model.ReminderEntity
import com.maya.android.app.service.MayaForegroundService
import com.maya.android.app.ui.screens.ActionLogScreen
import com.maya.android.app.ui.screens.ApiKeySetupScreen
import com.maya.android.app.ui.screens.MainVoiceScreen
import com.maya.android.app.ui.screens.MemoryScreen
import com.maya.android.app.ui.screens.RemindersScreen
import com.maya.android.app.ui.screens.SettingsScreen
import com.maya.android.app.ui.screens.VoicePickerScreen
import com.maya.android.app.ui.theme.MayaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    data object ApiKeySetup : Screen()
    data object MainVoice : Screen()
    data object VoicePicker : Screen()
    data object Memory : Screen()
    data object Reminders : Screen()
    data object ActionLog : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private var foregroundService: MayaForegroundService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MayaForegroundService.LocalBinder
            foregroundService = binder.service
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen for visual feedback on wake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        val app = application as MayaApplication
        val prefs = app.preferencesManager

        setContent {
            var activeMode by remember { mutableStateOf(prefs.activeMode) }
            val isGirlfriendMode = activeMode == PreferencesManager.MODE_GIRLFRIEND

            MayaTheme(isGirlfriendMode = isGirlfriendMode) {
                MayaRoot(
                    activity = this,
                    prefs = prefs,
                    app = app,
                    isGirlfriendMode = isGirlfriendMode,
                    onModeChanged = { newMode ->
                        activeMode = newMode
                        prefs.activeMode = newMode
                        // Reconnect session with new mode instructions if currently running
                        foregroundService?.let {
                            if (it.liveStateFlow.value != LiveState.IDLE) {
                                it.stopLiveSession()
                                it.startLiveSession()
                            }
                        }
                    },
                    getService = { foregroundService }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MayaForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("EXTRA_START_LISTENING", false)) {
            foregroundService?.startLiveSession()
        }
    }
}

@Composable
fun MayaRoot(
    activity: ComponentActivity,
    prefs: PreferencesManager,
    app: MayaApplication,
    isGirlfriendMode: Boolean,
    onModeChanged: (String) -> Unit,
    getService: () -> MayaForegroundService?
) {
    val scope = rememberCoroutineScope()
    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (prefs.geminiApiKey.isBlank() && !prefs.isOnboarded) Screen.ApiKeySetup else Screen.MainVoice
        )
    }

    // Observe Room data
    val memoriesFlow = remember { app.database.memoryDao().getAllMemoriesFlow() }
    val memories by memoriesFlow.collectAsState(initial = emptyList())

    val remindersFlow = remember { app.database.reminderDao().getAllRemindersFlow() }
    val reminders by remindersFlow.collectAsState(initial = emptyList())

    val logsFlow = remember { app.database.actionLogDao().getAllLogsFlow() }
    val logs by logsFlow.collectAsState(initial = emptyList())

    // Foreground service state observation
    val service = getService()
    val liveState by (service?.liveStateFlow ?: remember { MutableStateFlow(LiveState.IDLE) }).collectAsState()
    val userTranscript by (service?.userTranscriptFlow ?: remember { MutableStateFlow("") }).collectAsState()
    val mayaTranscript by (service?.mayaTranscriptFlow ?: remember { MutableStateFlow("") }).collectAsState()
    val statusMessage by (service?.statusMessageFlow ?: remember { MutableStateFlow("Maya is ready.") }).collectAsState()
    val pendingConfirmation by (service?.pendingConfirmationFlow ?: remember { MutableStateFlow(null) }).collectAsState()

    // Request necessary runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            MayaForegroundService.startService(activity)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        permissionsToRequest.add(Manifest.permission.SEND_SMS)
        permissionsToRequest.add(Manifest.permission.READ_CONTACTS)

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        } else {
            MayaForegroundService.startService(activity)
        }
    }

    // Deterministic Back Navigation
    BackHandler(enabled = currentScreen != Screen.MainVoice && currentScreen != Screen.ApiKeySetup) {
        currentScreen = Screen.MainVoice
    }

    when (val screen = currentScreen) {
        is Screen.ApiKeySetup -> {
            ApiKeySetupScreen(
                onSaveAndContinue = { name, key ->
                    prefs.userName = name
                    prefs.geminiApiKey = key
                    prefs.isOnboarded = true
                    currentScreen = Screen.MainVoice
                    MayaForegroundService.startService(activity)
                }
            )
        }

        is Screen.MainVoice -> {
            MainVoiceScreen(
                liveState = liveState,
                isGirlfriendMode = isGirlfriendMode,
                statusMessage = statusMessage,
                userTranscript = userTranscript,
                mayaTranscript = mayaTranscript,
                pendingConfirmation = pendingConfirmation,
                onToggleMode = {
                    val nextMode = if (isGirlfriendMode) PreferencesManager.MODE_JARVIS else PreferencesManager.MODE_GIRLFRIEND
                    onModeChanged(nextMode)
                },
                onMicClick = {
                    val svc = getService()
                    if (svc != null) {
                        if (liveState == LiveState.IDLE) {
                            svc.startLiveSession()
                        } else {
                            svc.stopLiveSession()
                        }
                    } else {
                        MayaForegroundService.startService(activity)
                    }
                },
                onSendPrompt = { promptText ->
                    val svc = getService()
                    if (svc != null) {
                        svc.queryPrompt(promptText)
                    } else {
                        MayaForegroundService.startService(activity)
                    }
                },
                onTestVoice = {
                    val svc = getService()
                    val testMsg = if (isGirlfriendMode) {
                        "Haanji baby! Main bilkul theek hoon aur aapki aawaaz sun rahi hoon."
                    } else {
                        "All voice and speech systems are operational, sir. Standing by."
                    }
                    svc?.speakDirectly(testMsg)
                },
                onStopAllActions = {
                    getService()?.abortAllAgentTasks()
                },
                onConfirmAction = {
                    scope.launch(Dispatchers.Default) {
                        pendingConfirmation?.onConfirm?.invoke()
                        getService()?.pendingConfirmationFlow?.value = null
                    }
                },
                onCancelAction = {
                    pendingConfirmation?.onCancel?.invoke()
                    getService()?.pendingConfirmationFlow?.value = null
                },
                onNavigateToMemories = { currentScreen = Screen.Memory },
                onNavigateToReminders = { currentScreen = Screen.Reminders },
                onNavigateToLogs = { currentScreen = Screen.ActionLog },
                onNavigateToSettings = { currentScreen = Screen.Settings }
            )
        }

        is Screen.VoicePicker -> {
            VoicePickerScreen(
                currentJarvisVoice = prefs.jarvisVoice,
                currentGirlfriendVoice = prefs.girlfriendVoice,
                onSelectJarvisVoice = { prefs.jarvisVoice = it },
                onSelectGirlfriendVoice = { prefs.girlfriendVoice = it },
                onBack = { currentScreen = Screen.Settings }
            )
        }

        is Screen.Memory -> {
            MemoryScreen(
                memories = memories,
                isGirlfriendMode = isGirlfriendMode,
                onAddMemory = { fact, category ->
                    scope.launch(Dispatchers.IO) {
                        app.database.memoryDao().insertMemory(
                            MemoryEntity(fact = fact, category = category)
                        )
                    }
                },
                onDeleteMemory = { id ->
                    scope.launch(Dispatchers.IO) {
                        app.database.memoryDao().deleteMemoryById(id)
                    }
                },
                onClearAllMemories = {
                    scope.launch(Dispatchers.IO) {
                        app.database.memoryDao().clearAll()
                    }
                },
                onBack = { currentScreen = Screen.MainVoice }
            )
        }

        is Screen.Reminders -> {
            RemindersScreen(
                reminders = reminders,
                isGirlfriendMode = isGirlfriendMode,
                onAddReminder = { title, delayMins ->
                    scope.launch(Dispatchers.IO) {
                        val svc = getService()
                        svc?.let {
                            it.startForegroundService(Intent(activity, MayaForegroundService::class.java))
                        }
                        val reminderExecutor = com.maya.android.app.tools.ReminderToolExecutor(activity, app.database.reminderDao())
                        reminderExecutor.setReminder(title, delayMins)
                    }
                },
                onDeleteReminder = { id ->
                    scope.launch(Dispatchers.IO) {
                        app.database.reminderDao().deleteReminderById(id)
                    }
                },
                onBack = { currentScreen = Screen.MainVoice }
            )
        }

        is Screen.ActionLog -> {
            ActionLogScreen(
                logs = logs,
                isGirlfriendMode = isGirlfriendMode,
                onClearLogs = {
                    scope.launch(Dispatchers.IO) {
                        app.database.actionLogDao().clearAll()
                    }
                },
                onBack = { currentScreen = Screen.MainVoice }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                prefs = prefs,
                isGirlfriendMode = isGirlfriendMode,
                onNavigateToVoicePicker = { currentScreen = Screen.VoicePicker },
                onDeleteAllData = {
                    scope.launch(Dispatchers.IO) {
                        app.database.memoryDao().clearAll()
                        app.database.reminderDao().clearAll()
                        app.database.actionLogDao().clearAll()
                        prefs.clearAll()
                        currentScreen = Screen.ApiKeySetup
                    }
                },
                onBack = { currentScreen = Screen.MainVoice }
            )
        }
    }
}
