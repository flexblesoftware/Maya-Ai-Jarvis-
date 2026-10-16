package com.maya.android.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.maya.android.app.data.AppDatabase
import com.maya.android.app.data.PreferencesManager

class MayaApplication : Application() {

    companion object {
        const val CHANNEL_FOREGROUND_SERVICE = "maya_foreground_service"
        const val CHANNEL_REMINDERS = "maya_reminders"
        const val CHANNEL_AGENT_ALERTS = "maya_agent_alerts"

        lateinit var instance: MayaApplication
            private set
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val fgChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "Maya Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Maya listening for wake words and active voice sessions."
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Maya Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for your scheduled reminders and timers."
                enableVibration(true)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_AGENT_ALERTS,
                "Maya Agent & Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming messages and phone automation tasks."
            }

            notificationManager.createNotificationChannel(fgChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
