package com.example.wq_learner1.ui.theme

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
    primary = NightPrimary,
    secondary = ColorMasteredDark,
    tertiary = ColorReviewingDark,
    error = ColorUnfamiliarDark,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightVariant,
    surfaceContainerHigh = Color(0xFF3F3F46),
    outline = NightOutline,
    outlineVariant = Color(0xFF52525B),
    onPrimary = Color(0xFF042F2E),
    onSecondary = Color(0xFF111111),
    onTertiary = Color(0xFF111111),
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    onSurfaceVariant = NightMuted,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SemanticSuccess,
    tertiary = SemanticInfo,
    error = SemanticError,
    background = NeutralBg,
    surface = NeutralSurface,
    surfaceVariant = NeutralVariant,
    surfaceContainerHigh = Color(0xFFEFEFEF),
    outline = NeutralOutline,
    outlineVariant = NeutralDivider,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NeutralInk,
    onSurface = NeutralInk,
    onSurfaceVariant = NeutralMuted,
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
