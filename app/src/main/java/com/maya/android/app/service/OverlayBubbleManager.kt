package com.maya.android.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

class OverlayBubbleManager(private val context: Context, private val onStopClicked: () -> Unit) {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isShowing = false

    fun showStopBubble() {
        if (isShowing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w("OverlayBubbleManager", "Overlay permission not granted")
            return
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 20
                y = 120
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }

            val stopButton = Button(context).apply {
                text = "STOP MAYA"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#E53935")) // Red panic button
                textSize = 14f
                setPadding(24, 16, 24, 16)
                setOnClickListener {
                    onStopClicked()
                    hideStopBubble()
                }
            }

            container.addView(stopButton)
            overlayView = container

            windowManager?.addView(overlayView, layoutParams)
            isShowing = true
            Log.i("OverlayBubbleManager", "Stop bubble displayed")
        } catch (e: Exception) {
            Log.w("OverlayBubbleManager", "Failed to display overlay stop bubble", e)
        }
    }

    fun hideStopBubble() {
        if (!isShowing || overlayView == null) return
        try {
            windowManager?.removeView(overlayView)
        } catch (e: Exception) {
            Log.w("OverlayBubbleManager", "Error removing overlay", e)
        }
        overlayView = null
        isShowing = false
    }
}
