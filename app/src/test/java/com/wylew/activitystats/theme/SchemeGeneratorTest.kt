package com.wylew.activitystats.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.min

class SchemeGeneratorTest {

    private val red = 0xFFC62828.toInt()
    private val green = 0xFF2E7D32.toInt()

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(a - b) % 360
        return min(d, 360 - d)
    }

    @Test
    fun labRoundTrip() {
        val lch = ColorScience.toLch(red)
        val back = ColorScience.fromLch(lch.l, lch.c, lch.h)!!
        assertEquals(red, back)
    }

    @Test
    fun tonesMatchLightness() {
        val p = TonalPalette.from(red)
        for (t in listOf(10, 30, 40, 50, 80, 90, 95)) {
            assertEquals("tone $t", t.toDouble(), ColorScience.tone(p.tone(t)), 1.0)
        }
        assertEquals(0xFF000000.toInt(), p.tone(0))
        assertEquals(0xFFFFFFFF.toInt(), p.tone(100))
    }

    @Test
    fun holidayKeepsRedPrimaryAndGreenAccent() {
        for (dark in listOf(false, true)) {
            val s = SchemeGenerator.generate(red, green, dark)
            val redHue = ColorScience.toLch(red).h
            val greenHue = ColorScience.toLch(green).h
            assertTrue(hueDistance(ColorScience.toLch(s.primary).h, redHue) < 15)
            assertTrue(hueDistance(ColorScience.toLch(s.tertiary).h, greenHue) < 15)
        }
    }

    @Test
    fun textPairsMeetContrast() {
        for (dark in listOf(false, true)) {
            for ((p, a) in Presets.pairs.map { it.primary to it.accent }) {
                val s = SchemeGenerator.generate(p, a, dark)
                val pairs = listOf(
                    s.primary to s.onPrimary,
                    s.primaryContainer to s.onPrimaryContainer,
                    s.tertiary to s.onTertiary,
                    s.tertiaryContainer to s.onTertiaryContainer,
                    s.surface to s.onSurface,
                    s.surfaceContainer to s.onSurfaceVariant,
                )
                pairs.forEach { (bg, fg) ->
                    assertTrue("dark=$dark contrast ${ColorScience.contrast(bg, fg)}", ColorScience.contrast(bg, fg) >= 4.5)
                }
            }
        }
    }

    @Test
    fun darkSurfacesAreDark() {
        val s = SchemeGenerator.generate(red, green, dark = true)
        assertTrue(ColorScience.tone(s.surface) < 10)
        assertTrue(ColorScience.tone(SchemeGenerator.generate(red, green, dark = false).surface) > 95)
    }
}
