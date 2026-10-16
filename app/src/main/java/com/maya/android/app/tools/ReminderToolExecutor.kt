package com.maya.android.app.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.maya.android.app.data.dao.ReminderDao
import com.maya.android.app.data.model.ReminderEntity
import com.maya.android.app.receiver.ReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderToolExecutor(
    private val context: Context,
    private val reminderDao: ReminderDao
) {

    suspend fun setReminder(title: String, delayMinutes: Int = 0, timeMillisTarget: Long = 0L): String = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext "Cannot set a reminder without a title."

        val targetTime = if (timeMillisTarget > System.currentTimeMillis()) {
            timeMillisTarget
        } else if (delayMinutes > 0) {
            System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        } else {
            System.currentTimeMillis() + (5 * 60 * 1000L) // Default 5 mins
        }

        val requestCode = (System.currentTimeMillis() % 100000).toInt()
        val reminder = ReminderEntity(
            title = title.trim(),
            timeMillis = targetTime,
            requestCode = requestCode
        )
        val id = reminderDao.insertReminder(reminder)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminder_id", id)
                putExtra("reminder_title", title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        targetTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                return@withContext "Reminder saved in app database, but exact alarm permission might be restricted: ${e.message}"
            }
        }

        val formattedDate = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault()).format(Date(targetTime))
        "Reminder scheduled for $formattedDate: \"$title\""
    }

    suspend fun listReminders(): String = withContext(Dispatchers.IO) {
        val reminders = reminderDao.getActiveReminders()
        if (reminders.isEmpty()) {
            "You have no upcoming reminders scheduled."
        } else {
            val format = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault())
            val list = reminders.take(10).mapIndexed { i, r ->
                "${i + 1}. \"${r.title}\" at ${format.format(Date(r.timeMillis))} (ID: ${r.id})"
            }
            "Upcoming reminders:\n" + list.joinToString("\n")
        }
    }

    suspend fun cancelReminder(query: String): String = withContext(Dispatchers.IO) {
        val id = query.toLongOrNull()
        if (id != null) {
            reminderDao.deleteReminderById(id)
            return@withContext "Reminder ID $id cancelled."
        }
        val reminders = reminderDao.getActiveReminders()
        val match = reminders.firstOrNull { it.title.contains(query, ignoreCase = true) }
        if (match != null) {
            reminderDao.deleteReminder(match)
            "Reminder \"${match.title}\" has been cancelled."
        } else {
            "Could not find any active reminder matching \"$query\"."
        }
    }

    fun setSystemAlarm(hour: Int, minutes: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minutes.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Maya Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Setting alarm for ${String.format(Locale.US, "%02d:%02d", hour, minutes)}: $message"
        } catch (e: Exception) {
            "Failed to launch alarm clock: ${e.message}"
        }
    }

    fun setSystemTimer(seconds: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "Maya Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Started system timer for $seconds seconds: $message"
        } catch (e: Exception) {
            "Failed to launch timer: ${e.message}"
        }
    }
}
