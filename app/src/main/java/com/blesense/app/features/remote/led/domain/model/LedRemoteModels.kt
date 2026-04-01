package com.blesense.app.features.remote.led.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.MusicNote

// ─────────────────────────────────────────────────────────────
// Protocols
// ─────────────────────────────────────────────────────────────
const val SECRET_KEY_SOLID     = 0xAA.toByte()
const val SECRET_KEY_ANIMATION = 0xBB.toByte()
const val SECRET_KEY_SLIDER    = 0xCC.toByte()

// ─────────────────────────────────────────────────────────────
// Domain Models
// ─────────────────────────────────────────────────────────────

data class Mood(
    val name: String,
    val colors: List<LEDCommand>,
    val interval: Int,
    val description: String,
    val gradient: List<Color>
)

enum class LEDCommand(
    val id: Byte,
    val displayName: String,
    val color: Color,
    val description: String,
    val mode: String,
    val companyId: Int
) {
    SOLID_MAGENTA      (1, "Magenta",     Color(0xFFFF00FF), "Solid Magenta Color",          "Solid",     0x0001),
    SOLID_YELLOW       (2, "Yellow",      Color(0xFFFFFF00), "Solid Yellow Color",            "Solid",     0x0002),
    SOLID_WHITE        (3, "White",       Color(0xFFFFFFFF), "Solid White Color",             "Solid",     0x0003),
    SOLID_RED          (4, "Red",         Color(0xFFFF0000), "Solid Red Color",               "Solid",     0x0004),
    SOLID_GREEN        (5, "Green",       Color(0xFF00FF00), "Solid Green Color",             "Solid",     0x0005),
    SOLID_BLUE         (6, "Blue",        Color(0xFF0000FF), "Solid Blue Color",              "Solid",     0x0006),
    SOLID_CYAN         (7, "Cyan",        Color(0xFF00FFFF), "Solid Cyan Color",              "Solid",     0x0007),
    INDIAN_FLAG        (8, "Indian Flag", Color(0xFFFF9933), "Saffron-White-Green Flag",      "Solid",     0x0008),
    SOLID_ORANGE       (9, "Orange",      Color(0xFFFF6600), "Solid Orange Color",            "Solid",     0x0009),
    SOLID_OFF          (0, "OFF",         Color.Black,       "Turn LEDs off",                 "Solid",     0x0000),

    ANIM_CYLON         (1, "Cylon Scanner",   Color(0xFFFF0000), "Red scanning back and forth",  "Animation", 0x0001),
    ANIM_POLICE        (2, "Police Strobe",   Color(0xFF0000FF), "Alternating red/blue flash",   "Animation", 0x0002),
    ANIM_BREATHER      (3, "Zen Breather",    Color(0xFF800080), "Pulsing purple breath",         "Animation", 0x0003),
    ANIM_RAINBOW       (4, "Spinning Rainbow",Color(0xFFFF69B4), "Rainbow colors cycling",        "Animation", 0x0004),
    ANIM_SHOOTING_STAR (5, "Shooting Star",   Color(0xFF87CEEB), "White comet with blue trail",   "Animation", 0x0005),
}

val MOOD_LIBRARY = listOf(
    Mood("Sunset Fade",    listOf(LEDCommand.SOLID_ORANGE, LEDCommand.SOLID_YELLOW, LEDCommand.SOLID_MAGENTA), 3, "Warm summer evening glow", listOf(Color(0xFFFF6600), Color(0xFFFFCC00), Color(0xFFFF00FF))),
    Mood("Ocean Pulse",    listOf(LEDCommand.SOLID_CYAN, LEDCommand.SOLID_BLUE), 4, "Deep rhythmic sea waves", listOf(Color(0xFF00FFFF), Color(0xFF0000FF))),
    Mood("Cyberpunk",      listOf(LEDCommand.SOLID_MAGENTA, LEDCommand.SOLID_CYAN), 2, "Neon night strobe", listOf(Color(0xFFFF00FF), Color(0xFF00FFFF))),
    Mood("Forest Zen",     listOf(LEDCommand.SOLID_GREEN, LEDCommand.SOLID_WHITE), 5, "Calm focused atmosphere", listOf(Color(0xFF00FF00), Color(0xFFEEEEEE))),
    Mood("Imperial",       listOf(LEDCommand.INDIAN_FLAG, LEDCommand.SOLID_YELLOW), 3, "Royal colors", listOf(Color(0xFFFF9933), Color(0xFFFFCC00)))
)

enum class RemoteType(
    val displayName: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    REMOTE_1("Colors",     Color(0xFF673AB7), Icons.Default.Palette, "Solid colors (Key: 0xAA)"),
    REMOTE_2("Animations", Color(0xFFE91E63), Icons.Default.AutoFixHigh, "Patterns (Key: 0xBB)"),
    REMOTE_3("Spectrum",   Color(0xFF2196F3), Icons.Default.InvertColors, "HSV Picker (Key: 0xAA)"),
    REMOTE_4("Vibes",      Color(0xFFFF9800), Icons.Default.MusicNote, "Audio & Moods")
}

data class AdvertisingHistory(
    val label: String,
    val timestamp: Long,
    val remoteType: RemoteType,
    val companyId: Int,
    val dataHex: String
)
