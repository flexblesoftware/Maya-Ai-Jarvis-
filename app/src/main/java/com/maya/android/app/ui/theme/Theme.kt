package com.maya.android.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = AmoledBackground,
    primaryContainer = JarvisBlue,
    onPrimaryContainer = TextPrimary,
    secondary = JarvisBlue,
    onSecondary = TextPrimary,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

private val GirlfriendColorScheme = darkColorScheme(
    primary = GirlfriendPink,
    onPrimary = AmoledBackground,
    primaryContainer = GirlfriendRose,
    onPrimaryContainer = TextPrimary,
    secondary = GirlfriendWarm,
    onSecondary = TextPrimary,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun MayaTheme(
    isGirlfriendMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isGirlfriendMode) GirlfriendColorScheme else JarvisColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AmoledBackground.toArgb()
            window.navigationBarColor = AmoledBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
