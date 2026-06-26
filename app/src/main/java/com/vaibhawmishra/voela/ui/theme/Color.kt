package com.vaibhawmishra.voela.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand — purple is the accent, not the whole palette
val Purple = Color(0xFF8B5CF6)
val PurpleDeep = Color(0xFF7C3AED)
val PurpleGlow = Color(0xFFA78BFA)

// A user-selectable accent. `base` is the main brand color, `glow` a lighter
// variant for highlights/gradients, `deep` a darker one, and `onAccent` the
// text/icon color that sits on top of `base` with safe contrast (white for the
// darker accents, near-black for the bright ones). Dark mode only.
data class Accent(
    val key: String,
    val label: String,
    val base: Color,
    val glow: Color,
    val deep: Color,
    val onAccent: Color,
)

val PurpleAccent = Accent("purple", "Purple", Purple, PurpleGlow, PurpleDeep, Color.White)
val BlueAccent = Accent("blue", "Blue", Color(0xFF3B82F6), Color(0xFF60A5FA), Color(0xFF2563EB), Color.White)
val TealAccent = Accent("teal", "Teal", Color(0xFF2DD4BF), Color(0xFF5EEAD4), Color(0xFF14B8A6), Color(0xFF06201D))
val EmeraldAccent = Accent("emerald", "Emerald", Color(0xFF34D399), Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF052016))
val AmberAccent = Accent("amber", "Amber", Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFF231400))
val RoseAccent = Accent("rose", "Rose", Color(0xFFFB7185), Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFF2B070D))

// Purple first so it is the default everywhere.
val Accents = listOf(PurpleAccent, BlueAccent, TealAccent, EmeraldAccent, AmberAccent, RoseAccent)
val DefaultAccent = PurpleAccent

fun accentFor(key: String?): Accent = Accents.firstOrNull { it.key == key } ?: DefaultAccent

// Provided by VoelaTheme; read with LocalAccent.current from any composable.
val LocalAccent = staticCompositionLocalOf { DefaultAccent }

// Surfaces — dark only
val Background = Color(0xFF070708)
val Surface = Color(0xFF141417)
val SurfaceElevated = Color(0xFF29292F)
val Outline = Color(0xFF242428)

// Text
val TextPrimary = Color(0xFFF0F0F0)
val TextSecondary = Color(0xFFA0A0A0)

// Functional — consistent meaning across every screen
val DownloadGreen = Color(0xFF34D399)
val ShareBlue = Color(0xFF60A5FA)
val VocalsAmber = Color(0xFFF59E0B)
val InstrumentalTeal = Color(0xFF2DD4BF)
val RemainderYellow = Color(0xFFD4C24A)
val Warning = Color(0xFFF87171)

// Brand red for YouTube affordances
val YouTubeRed = Color(0xFFF0312E)
