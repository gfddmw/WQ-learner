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
    surfaceVariant = Color(0xFF1F1F1F),
    outline = Color(0xFF333333),
    onPrimary = Color(0xFF111111),
    onSecondary = Color(0xFF111111),
    onTertiary = Color(0xFF111111),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF9CA3AF),
)

private val LightColorScheme = lightColorScheme(
    primary = WorkbenchGreen,
    secondary = ColorMastered,
    tertiary = WorkbenchBlue,
    error = WorkbenchRed,
    background = WorkbenchPaper,
    surface = WorkbenchSurface,
    surfaceVariant = WorkbenchMist,
    outline = WorkbenchLine,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = WorkbenchInk,
    onSurface = WorkbenchInk,
    onSurfaceVariant = WorkbenchMuted,
)

@Composable
fun WQlearner1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        content = content,
    )
}
