package com.maya.android.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maya.android.app.ai.LiveState
import com.maya.android.app.ui.theme.GirlfriendPink
import com.maya.android.app.ui.theme.GirlfriendRose
import com.maya.android.app.ui.theme.JarvisBlue
import com.maya.android.app.ui.theme.JarvisCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicVoiceOrb(
    state: LiveState,
    isGirlfriendMode: Boolean,
    modifier: Modifier = Modifier,
    orbSize: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

    // Core breathing animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    LiveState.SPEAKING -> 600
                    LiveState.THINKING -> 800
                    LiveState.LISTENING -> 1200
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Wave ring animation
    val ringExpansion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingExpansion"
    )

    // Rotation for thinking/active state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Colors
    val primaryColor = if (isGirlfriendMode) GirlfriendPink else JarvisCyan
    val secondaryColor = if (isGirlfriendMode) GirlfriendRose else JarvisBlue
    val errorColor = Color(0xFFFF3D00)
    val activeColor = if (state == LiveState.ERROR) errorColor else primaryColor

    Box(
        modifier = modifier
            .size(orbSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.width / 3.4f) * pulseScale

            // 1. Outermost expanding ripple (when listening or speaking)
            if (state == LiveState.LISTENING || state == LiveState.SPEAKING) {
                val outerRadius = baseRadius + (ringExpansion * 65f)
                val alpha = (1f - ringExpansion).coerceIn(0f, 1f) * 0.4f
                drawCircle(
                    color = activeColor.copy(alpha = alpha),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 2. Soft background aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = center
            )

            // 3. Middle luminous gradient orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        activeColor,
                        secondaryColor.copy(alpha = 0.8f)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 4. Orbital acoustic particle spikes around circumference
            val particleCount = if (state == LiveState.SPEAKING) 24 else 12
            val angleStep = (2 * Math.PI / particleCount).toFloat()
            val radAngle = Math.toRadians(rotationAngle.toDouble()).toFloat()

            for (i in 0 until particleCount) {
                val angle = (i * angleStep) + (if (state == LiveState.THINKING) radAngle else 0f)
                val spikeLen = if (state == LiveState.SPEAKING) {
                    (sin(angle * 3 + rotationAngle * 0.1f) * 18f) + 8f
                } else if (state == LiveState.LISTENING) {
                    (cos(angle * 2 + rotationAngle * 0.05f) * 10f) + 6f
                } else {
                    4f
                }

                val startRadius = baseRadius + 4f
                val endRadius = startRadius + spikeLen

                val p1 = Offset(
                    center.x + (cos(angle) * startRadius),
                    center.y + (sin(angle) * startRadius)
                )
                val p2 = Offset(
                    center.x + (cos(angle) * endRadius),
                    center.y + (sin(angle) * endRadius)
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = p1,
                    end = p2,
                    strokeWidth = 2.5f
                )
            }

            // 5. Crisp inner core ring
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = baseRadius * 0.28f,
                center = center
            )
        }
    }
}
