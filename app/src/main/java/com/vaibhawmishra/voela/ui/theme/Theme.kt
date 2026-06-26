package com.vaibhawmishra.voela.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun VoelaTheme(accent: Accent = DefaultAccent, content: @Composable () -> Unit) {
    val colorScheme = remember(accent) {
        darkColorScheme(
            primary = accent.base,
            onPrimary = accent.onAccent,
            primaryContainer = accent.deep,
            secondary = accent.glow,
            background = Background,
            onBackground = TextPrimary,
            surface = Surface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceElevated,
            onSurfaceVariant = TextSecondary,
            outline = Outline,
            error = Warning,
        )
    }
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
