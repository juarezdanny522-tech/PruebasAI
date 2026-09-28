package com.pruebasai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CB8FF),
    onPrimary = Color(0xFF0B2359),
    primaryContainer = Color(0xFF27428F),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFBDC6DC),
    onSecondary = Color(0xFF25303F),
    secondaryContainer = Color(0xFF3B4758),
    onSecondaryContainer = Color(0xFFDCE5F9),
    tertiary = Color(0xFFE2B8E8),
    onTertiary = Color(0xFF402745),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE3E7EE),
    surface = Color(0xFF141820),
    onSurface = Color(0xFFE3E7EE),
    surfaceVariant = Color(0xFF202632),
    onSurfaceVariant = Color(0xFFC1C7D3),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B61C4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF565E71),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
)

@Composable
fun PruebasAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
