package com.maya.android.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

data class CompactScreenNode(
    val id: String,
    val text: String,
    val contentDescription: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: String,
    val centerX: Int,
    val centerY: Int
)

class MayaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MayaAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("MayaAccessibility", "Maya Accessibility Service connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event monitoring
    }

    override fun onInterrupt() {
        Log.w("MayaAccessibility", "Maya Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i("MayaAccessibility", "Maya Accessibility Service destroyed")
    }

    /**
     * Traverses the active window and returns a compact list of actionable/visible nodes.
     */
    fun getCompactScreenNodes(): List<CompactScreenNode> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<CompactScreenNode>()
        val outBounds = Rect()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            // Safety filter: never capture password fields
            if (node.isPassword) {
                return
            }

            node.getBoundsInScreen(outBounds)
            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val viewId = node.viewIdResourceName ?: ""

            // Only capture nodes that have visual text, a label, or are clickable/editable
            if (text.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isEditable) {
                val cx = outBounds.centerX()
                val cy = outBounds.centerY()
                // Only include if bounds are reasonable on screen
                if (outBounds.width() > 0 && outBounds.height() > 0) {
                    list.add(
                        CompactScreenNode(
                            id = viewId.substringAfterLast("/"),
                            text = text,
                            contentDescription = desc,
                            isClickable = node.isClickable,
                            isEditable = node.isEditable,
                            bounds = "[${outBounds.left},${outBounds.top} to ${outBounds.right},${outBounds.bottom}]",
                            centerX = cx,
                            centerY = cy
                        )
                    )
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(root)
        } catch (e: Exception) {
            Log.w("MayaAccessibility", "Error traversing window content", e)
        }

        return list.take(35) // Keep compact for model context
    }

    suspend fun performTapGesture(x: Int, y: Int): Boolean {
        if (x <= 0 || y <= 0) return false
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        if (!dispatched) return false
        return withTimeoutOrNull(2000) { deferred.await() } ?: false
    }

    suspend fun performScrollGesture(direction: String): Boolean {
        val root = rootInActiveWindow
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val startX = screenWidth / 2f
        val startY = screenHeight / 2f
        val endY = when (direction.lowercase()) {
            "down", "forward" -> screenHeight * 0.2f
            "up", "backward" -> screenHeight * 0.8f
            else -> screenHeight * 0.2f
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        if (!dispatched) return false
        return withTimeoutOrNull(2000) { deferred.await() } ?: false
    }

    fun setTextOnActiveNode(text: String, elementId: String = ""): Boolean {
        val root = rootInActiveWindow ?: return false

        fun findAndSet(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isPassword) return false // Safety rule

            val matchesId = elementId.isNotBlank() && (node.viewIdResourceName?.contains(elementId, ignoreCase = true) == true)
            if (matchesId || node.isFocused || node.isEditable) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (success) return true
            }

            for (i in 0 until node.childCount) {
                if (findAndSet(node.getChild(i))) return true
            }
            return false
        }

        return findAndSet(root)
    }

    fun performGlobalActionKey(key: String): Boolean {
        val action = when (key.lowercase()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents", "recent_apps" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            else -> GLOBAL_ACTION_BACK
        }
        return performGlobalAction(action)
    }
}
