package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Classic Windows XP / MSN Messenger 7.5 Frutiger Aero Palette
val MsnAzurePrimary = Color(0xFF0F62C9)
val MsnSkyLight = Color(0xFF5FA8F5)
val MsnIceBlue = Color(0xFFDCEBFC)
val MsnFrostSurface = Color(0xFFF4F8FE)
val MsnWindowBorder = Color(0xFF8BB4E6)
val MsnHeaderTop = Color(0xFF1A73E8)
val MsnHeaderBottom = Color(0xFF0B4DA2)

val MsnGreenOnline = Color(0xFF2EB840)
val MsnOrangeAway = Color(0xFFF59E0B)
val MsnRedBusy = Color(0xFFE53935)
val MsnGrayOffline = Color(0xFF8C97A8)

val MsnAmberAccent = Color(0xFFFF8C00)
val MsnDarkBackground = Color(0xFF0E1726)
val MsnDarkSurface = Color(0xFF172438)
val MsnDarkSurfaceVariant = Color(0xFF1F304A)
val MsnDarkBorder = Color(0xFF334C70)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF1A1A1A)): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        when (cleaned.length) {
            6 -> Color(android.graphics.Color.parseColor("#FF$cleaned"))
            8 -> Color(android.graphics.Color.parseColor("#$cleaned"))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun Color.toHexRgb(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}
