package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.ui.home.ProcessType
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.VocalsAmber
import com.vaibhawmishra.voela.ui.theme.YouTubeRed

// One accent + glyph per action type, reused by Recents and Library so the visual
// language matches the Select Feature screen (mic = vocals, video = YouTube).
val ProcessType.accent: Color
    get() = when (this) {
        ProcessType.VOCAL_REMOVAL -> VocalsAmber
        ProcessType.EXTRACTION -> YouTubeRed
        ProcessType.AUDIO_SPLIT -> InstrumentalTeal
    }

val ProcessType.icon: ImageVector
    get() = when (this) {
        ProcessType.VOCAL_REMOVAL -> Icons.Outlined.Mic
        ProcessType.EXTRACTION -> Icons.Outlined.SmartDisplay
        ProcessType.AUDIO_SPLIT -> Icons.Outlined.ContentCut
    }

// Rounded-square icon tile standing in for a thumbnail. Extractions show the real
// YouTube mark (solid red + white play); other types use a soft tinted glyph.
@Composable
fun TypeIconTile(type: ProcessType, modifier: Modifier = Modifier, tile: Dp = 56.dp) {
    if (type == ProcessType.EXTRACTION) {
        Box(
            modifier.size(tile).clip(RoundedCornerShape(16.dp)).background(YouTubeRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(tile * 0.5f))
        }
    } else {
        Box(
            modifier.size(tile).clip(RoundedCornerShape(16.dp)).background(type.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(type.icon, null, tint = type.accent, modifier = Modifier.size(tile * 0.46f))
        }
    }
}

// Soft tonal pill: faint tinted fill + a gently lightened label, so even the brand
// red reads as a calm tag rather than an alert.
@Composable
fun TypeChip(type: ProcessType) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(type.accent.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            type.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = lerp(type.accent, Color.White, 0.45f),
        )
    }
}
