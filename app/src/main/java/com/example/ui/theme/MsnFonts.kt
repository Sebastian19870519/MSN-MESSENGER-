package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

data class MsnFontOption(
    val id: String,
    val displayName: String,
    val category: String,
    val fontFamily: FontFamily
)

object MsnFontCatalog {
    val openSans = FontFamily(Font(R.font.open_sans, FontWeight.Normal))
    val comicNeue = FontFamily(Font(R.font.comic_neue, FontWeight.Normal))
    val pacifico = FontFamily(Font(R.font.pacifico, FontWeight.Normal))
    val dancingScript = FontFamily(Font(R.font.dancing_script, FontWeight.Normal))
    val playfairDisplay = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
    val merriweather = FontFamily(Font(R.font.merriweather, FontWeight.Normal))
    val jetBrainsMono = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
    val courierPrime = FontFamily(Font(R.font.courier_prime, FontWeight.Normal))
    val pressStart2P = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
    val orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
    val fredoka = FontFamily(Font(R.font.fredoka, FontWeight.Normal))
    val caveat = FontFamily(Font(R.font.caveat, FontWeight.Normal))
    val notoSans = FontFamily(Font(R.font.noto_sans, FontWeight.Normal))

    val options: List<MsnFontOption> = listOf(
        MsnFontOption("open_sans", "Open Sans (MSN Classic)", "Sans-Serif", openSans),
        MsnFontOption("comic_neue", "Comic Neue (Retro MSN)", "Playful", comicNeue),
        MsnFontOption("fredoka", "Fredoka Rounded", "Playful", fredoka),
        MsnFontOption("pacifico", "Pacifico Brush", "Script", pacifico),
        MsnFontOption("dancing_script", "Dancing Script", "Script", dancingScript),
        MsnFontOption("caveat", "Caveat Marker", "Handwritten", caveat),
        MsnFontOption("playfair_display", "Playfair Display", "Serif", playfairDisplay),
        MsnFontOption("merriweather", "Merriweather", "Serif", merriweather),
        MsnFontOption("courier_prime", "Courier Prime", "Typewriter", courierPrime),
        MsnFontOption("jetbrains_mono", "JetBrains Mono", "Monospace", jetBrainsMono),
        MsnFontOption("orbitron", "Orbitron Sci-Fi", "Display", orbitron),
        MsnFontOption("press_start_2p", "Press Start 2P", "Pixel Retro", pressStart2P),
        MsnFontOption("noto_sans", "Noto Sans (Global)", "Multilingual", notoSans),
        MsnFontOption("sys_sans", "System Sans-Serif", "System", FontFamily.SansSerif),
        MsnFontOption("sys_serif", "System Serif", "System", FontFamily.Serif),
        MsnFontOption("sys_mono", "System Monospace", "System", FontFamily.Monospace),
        MsnFontOption("sys_cursive", "System Cursive", "System", FontFamily.Cursive),
        MsnFontOption("sys_default", "System Default", "System", FontFamily.Default)
    )

    fun resolve(fontId: String): FontFamily {
        return options.firstOrNull { it.id == fontId }?.fontFamily ?: openSans
    }
}
