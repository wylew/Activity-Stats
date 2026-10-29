package com.wylew.activitystats.theme

/** Every Material 3 color role, as ARGB ints. Converted to a Compose ColorScheme in ui/theme. */
data class GeneratedScheme(
    val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
    val inversePrimary: Int,
    val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
    val tertiary: Int, val onTertiary: Int, val tertiaryContainer: Int, val onTertiaryContainer: Int,
    val error: Int, val onError: Int, val errorContainer: Int, val onErrorContainer: Int,
    val background: Int, val onBackground: Int,
    val surface: Int, val onSurface: Int, val surfaceVariant: Int, val onSurfaceVariant: Int,
    val surfaceTint: Int, val inverseSurface: Int, val inverseOnSurface: Int,
    val outline: Int, val outlineVariant: Int, val scrim: Int,
    val surfaceBright: Int, val surfaceDim: Int,
    val surfaceContainerLowest: Int, val surfaceContainerLow: Int, val surfaceContainer: Int,
    val surfaceContainerHigh: Int, val surfaceContainerHighest: Int,
)

/**
 * Builds a full light or dark scheme from two user-picked colors.
 *
 * - Primary palette: the first color's hue, kept vivid.
 * - Tertiary palette: the second color's hue (so red + green gives red buttons and green accents).
 * - Secondary: the first hue, muted, for chips and selected states.
 * - Neutrals: a faint tint of the first hue, so surfaces feel related without being colored.
 *
 * Tones follow Material's standard mapping, which guarantees text contrast on every pairing.
 */
object SchemeGenerator {

    fun generate(primarySeed: Int, accentSeed: Int, dark: Boolean): GeneratedScheme {
        val p = ColorScience.toLch(primarySeed)
        val a = ColorScience.toLch(accentSeed)
        val primary = TonalPalette(p.h, p.c.coerceIn(40.0, 90.0))
        val tertiary = TonalPalette(a.h, a.c.coerceIn(36.0, 90.0))
        val secondary = TonalPalette(p.h, 18.0)
        val neutral = TonalPalette(p.h, 5.0)
        val neutralVariant = TonalPalette(p.h, 10.0)
        val error = TonalPalette(40.0, 80.0)
        return if (dark) build(primary, secondary, tertiary, error, neutral, neutralVariant, DARK)
        else build(primary, secondary, tertiary, error, neutral, neutralVariant, LIGHT)
    }

    private class Tones(
        val accent: Int, val onAccent: Int, val container: Int, val onContainer: Int, val inversePrimary: Int,
        val background: Int, val onBackground: Int, val surfaceVariant: Int, val onSurfaceVariant: Int,
        val inverseSurface: Int, val inverseOnSurface: Int, val outline: Int, val outlineVariant: Int,
        val bright: Int, val dim: Int,
        val lowest: Int, val low: Int, val mid: Int, val high: Int, val highest: Int,
    )

    private val LIGHT = Tones(
        accent = 40, onAccent = 100, container = 90, onContainer = 10, inversePrimary = 80,
        background = 98, onBackground = 10, surfaceVariant = 90, onSurfaceVariant = 30,
        inverseSurface = 20, inverseOnSurface = 95, outline = 50, outlineVariant = 80,
        bright = 98, dim = 87, lowest = 100, low = 96, mid = 94, high = 92, highest = 90,
    )

    private val DARK = Tones(
        accent = 80, onAccent = 20, container = 30, onContainer = 90, inversePrimary = 40,
        background = 6, onBackground = 90, surfaceVariant = 30, onSurfaceVariant = 80,
        inverseSurface = 90, inverseOnSurface = 20, outline = 60, outlineVariant = 30,
        bright = 24, dim = 6, lowest = 4, low = 10, mid = 12, high = 17, highest = 22,
    )

    private fun build(
        pr: TonalPalette, se: TonalPalette, te: TonalPalette, er: TonalPalette,
        n: TonalPalette, nv: TonalPalette, t: Tones,
    ) = GeneratedScheme(
        primary = pr.tone(t.accent), onPrimary = pr.tone(t.onAccent),
        primaryContainer = pr.tone(t.container), onPrimaryContainer = pr.tone(t.onContainer),
        inversePrimary = pr.tone(t.inversePrimary),
        secondary = se.tone(t.accent), onSecondary = se.tone(t.onAccent),
        secondaryContainer = se.tone(t.container), onSecondaryContainer = se.tone(t.onContainer),
        tertiary = te.tone(t.accent), onTertiary = te.tone(t.onAccent),
        tertiaryContainer = te.tone(t.container), onTertiaryContainer = te.tone(t.onContainer),
        error = er.tone(t.accent), onError = er.tone(t.onAccent),
        errorContainer = er.tone(t.container), onErrorContainer = er.tone(t.onContainer),
        background = n.tone(t.background), onBackground = n.tone(t.onBackground),
        surface = n.tone(t.background), onSurface = n.tone(t.onBackground),
        surfaceVariant = nv.tone(t.surfaceVariant), onSurfaceVariant = nv.tone(t.onSurfaceVariant),
        surfaceTint = pr.tone(t.accent),
        inverseSurface = n.tone(t.inverseSurface), inverseOnSurface = n.tone(t.inverseOnSurface),
        outline = nv.tone(t.outline), outlineVariant = nv.tone(t.outlineVariant),
        scrim = n.tone(0),
        surfaceBright = n.tone(t.bright), surfaceDim = n.tone(t.dim),
        surfaceContainerLowest = n.tone(t.lowest), surfaceContainerLow = n.tone(t.low),
        surfaceContainer = n.tone(t.mid), surfaceContainerHigh = n.tone(t.high),
        surfaceContainerHighest = n.tone(t.highest),
    )
}
