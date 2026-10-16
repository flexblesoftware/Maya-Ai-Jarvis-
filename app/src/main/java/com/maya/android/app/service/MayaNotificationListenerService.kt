package com.maya.android.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class IncomingMessageEvent(
    val id: String,
    val packageName: String,
    val sender: String,
    val text: String,
    val postTime: Long,
    val replyAction: Notification.Action?
)

class MayaNotificationListenerService : NotificationListenerService() {

    companion object {
        var instance: MayaNotificationListenerService? = null
            private set

        var messageListener: ((IncomingMessageEvent) -> Unit)? = null

        fun isServiceConnected(): Boolean = instance != null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i("MayaNotificationListener", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName
        // Monitor standard messaging packages (WhatsApp, Telegram, Signal, Google Messages, SMS)
        val messagingApps = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.facebook.orca",
            "org.thoughtcrime.securesms"
        )

        if (!messagingApps.contains(pkg)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (text.isBlank() || title.isBlank()) return

        // Find wearable / direct reply action if present
        var replyAction: Notification.Action? = null
        notification.actions?.forEach { action ->
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                replyAction = action
            }
        }

        val event = IncomingMessageEvent(
            id = sbn.key,
            packageName = pkg,
            sender = title,
            text = text,
            postTime = sbn.postTime,
            replyAction = replyAction
        )

        messageListener?.invoke(event)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.w("MayaNotificationListener", "Notification listener disconnected")
    }

    fun replyToNotification(action: Notification.Action, replyText: String): Boolean {
        try {
            val remoteInputs = action.remoteInputs ?: return false
            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            action.actionIntent.send(this, 0, intent)
            Log.i("MayaNotificationListener", "RemoteInput reply sent successfully: $replyText")
            return true
        } catch (e: Exception) {
            Log.e("MayaNotificationListener", "Failed to send RemoteInput reply", e)
            return false
        }
    }
}
