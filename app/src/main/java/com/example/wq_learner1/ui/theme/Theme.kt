package com.example.wq_learner1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NightBlue,
    secondary = ColorMasteredDark,
    tertiary = ColorReviewingDark,
    error = ColorUnfamiliarDark,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = Color(0xFF232A31),
    outline = Color(0xFF334155),
    onPrimary = Color(0xFF12161A),
    onSecondary = Color(0xFF12161A),
    onTertiary = Color(0xFF12161A),
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF90A4AE),
)

private val LightColorScheme = lightColorScheme(
    primary = WorkbenchGreen,
    secondary = ColorMastered,
    tertiary = WorkbenchBlue,
    error = WorkbenchRed,
    background = WorkbenchPaper,
    surface = WorkbenchSurface,
    surfaceVariant = WorkbenchSurfaceWarm,
    outline = WorkbenchLine,
    onPrimary = WorkbenchSurface,
    onSecondary = WorkbenchSurface,
    onTertiary = WorkbenchSurface,
    onBackground = WorkbenchInk,
    onSurface = WorkbenchInk,
    onSurfaceVariant = WorkbenchMuted,
)

@Composable
fun WQlearner1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
