package com.vaibhawmishra.voela.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoelaColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = PurpleDeep,
    secondary = PurpleGlow,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = Warning,
)

@Composable
fun VoelaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoelaColorScheme,
        typography = Typography,
        content = content
    )
}
