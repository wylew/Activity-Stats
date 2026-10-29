package com.wylew.activitystats.theme

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class ColorSource { WALLPAPER, CUSTOM }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val source: ColorSource = ColorSource.WALLPAPER,
    val primarySeed: Int = Presets.CLASSIC.primary,
    val accentSeed: Int = Presets.CLASSIC.accent,
)

data class ColorPair(val name: String, val primary: Int, val accent: Int)

object Presets {
    private fun c(hex: Long) = hex.toInt()

    val CLASSIC = ColorPair("Classic", c(0xFF4459A9), c(0xFF8E4D2F))

    val pairs = listOf(
        CLASSIC,
        ColorPair("Holiday", c(0xFFC62828), c(0xFF2E7D32)),
        ColorPair("Ocean", c(0xFF0B6E99), c(0xFF00897B)),
        ColorPair("Sunset", c(0xFFE65100), c(0xFF8E24AA)),
        ColorPair("Forest", c(0xFF33691E), c(0xFF8D6E63)),
        ColorPair("Berry", c(0xFFAD1457), c(0xFF5E35B1)),
    )

    /** Quick picks for a single color slot. */
    val swatches = listOf(
        0xFFC62828, 0xFFE64A19, 0xFFEF6C00, 0xFFF9A825, 0xFF9E9D24, 0xFF2E7D32, 0xFF00796B,
        0xFF0097A7, 0xFF1565C0, 0xFF3949AB, 0xFF7B1FA2, 0xFFC2185B, 0xFF6D4C41, 0xFF546E7A,
    ).map { it.toInt() }

    /** A vivid color at the given hue, for the hue slider. */
    fun fromHue(hue: Float): Int = TonalPalette(hue.toDouble(), 70.0).tone(50)

    fun hueOf(argb: Int): Float = ColorScience.toLch(argb).h.toFloat()
}
