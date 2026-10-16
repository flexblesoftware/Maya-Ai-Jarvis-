package com.maya.android.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maya.android.app.MainActivity
import com.maya.android.app.MayaApplication
import com.maya.android.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("BootReceiver", "Device rebooted, rescheduling active reminders...")

            val app = context.applicationContext as? MayaApplication ?: return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val activeReminders = app.database.reminderDao().getActiveReminders()
                for (reminder in activeReminders) {
                    val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra("reminder_id", reminder.id)
                        putExtra("reminder_title", reminder.title)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reminder.requestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminder.timeMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                reminder.timeMillis,
                                pendingIntent
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("BootReceiver", "Error rescheduling reminder ${reminder.id}", e)
                    }
                }

                // If wake-word was enabled, prompt user to tap once on Android 14+ to resume microphone foreground service
                if (app.preferencesManager.wakeWordEnabled) {
                    showRebootNotice(context)
                }
            }
        }
    }

    private fun showRebootNotice(context: Context) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MayaApplication.CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Maya Voice Assistant")
            .setContentText("Tap to resume background wake-word listening after reboot.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(999, notification)
        } catch (e: Exception) {
            Log.w("BootReceiver", "Error displaying reboot notification", e)
        }
    }
}
