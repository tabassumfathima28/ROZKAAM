package com.rozkaam.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OldBurgundy,
    onPrimary = BackgroundLight,
    primaryContainer = Buttermilk,
    onPrimaryContainer = OldBurgundy,
    secondary = PastelBlue,
    onSecondary = OldBurgundy,
    secondaryContainer = PastelBlue,
    onSecondaryContainer = OldBurgundy,
    tertiary = MutedGreen,
    onTertiary = BackgroundLight,
    background = BackgroundLight,
    onBackground = OldBurgundy,
    surface = BackgroundLight,
    onSurface = OldBurgundy,
    surfaceVariant = Buttermilk,
    onSurfaceVariant = OldBurgundy,
    error = MutedRed,
    onError = BackgroundLight,
    outline = OutlineColor
)

@Composable
fun RozkaamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Rozkaam uses the curated A4 design system palette
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RozkaamTypography,
        shapes = Shapes,
        content = content
    )
}
