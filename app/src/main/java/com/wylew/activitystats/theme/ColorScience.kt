package com.wylew.activitystats.theme

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Just enough color science to build Material-style tonal palettes, with no Android deps.
 *
 * Material's HCT space uses CIELAB L* for "tone", so tones here line up with Material's
 * (tone 40 on tone 100 always clears 4.5:1 contrast). Hue and chroma come from CIE LCh(ab),
 * a close stand-in for HCT's CAM16 hue/chroma.
 */
object ColorScience {

    private const val E = 216.0 / 24389.0
    private const val K = 24389.0 / 27.0
    private const val XN = 95.047
    private const val YN = 100.0
    private const val ZN = 108.883

    data class Lch(val l: Double, val c: Double, val h: Double)

    fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    fun toLch(argb: Int): Lch {
        val r = linearize((argb shr 16) and 0xFF)
        val g = linearize((argb shr 8) and 0xFF)
        val b = linearize(argb and 0xFF)
        val x = (0.41233895 * r + 0.35762064 * g + 0.18051042 * b) * 100
        val y = (0.2126 * r + 0.7152 * g + 0.0722 * b) * 100
        val z = (0.01932141 * r + 0.11916382 * g + 0.95034478 * b) * 100
        val fx = f(x / XN)
        val fy = f(y / YN)
        val fz = f(z / ZN)
        val l = 116 * fy - 16
        val a = 500 * (fx - fy)
        val bb = 200 * (fy - fz)
        val h = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
        return Lch(l, hypot(a, bb), h)
    }

    /** The color at (L*, C, h), or null if it falls outside sRGB. */
    fun fromLch(l: Double, c: Double, h: Double): Int? {
        val rad = Math.toRadians(h)
        val a = c * cos(rad)
        val b = c * sin(rad)
        val fy = (l + 16) / 116
        val fx = fy + a / 500
        val fz = fy - b / 200
        val x = XN * finv(fx)
        val y = YN * (if (l > K * E) fy * fy * fy else l / K)
        val z = ZN * finv(fz)
        val xs = x / 100
        val ys = y / 100
        val zs = z / 100
        val rl = 3.2413774 * xs - 1.5376652 * ys - 0.49885366 * zs
        val gl = -0.96924364 * xs + 1.8759676 * ys + 0.04155506 * zs
        val bl = 0.05562093 * xs - 0.20395525 * ys + 1.0571799 * zs
        val eps = 1e-4
        if (rl < -eps || rl > 1 + eps || gl < -eps || gl > 1 + eps || bl < -eps || bl > 1 + eps) return null
        return argb(delinearize(rl), delinearize(gl), delinearize(bl))
    }

    /** Tone = L* (0 black … 100 white). */
    fun tone(argb: Int): Double = toLch(argb).l

    /** WCAG contrast ratio between two colors. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val (hi, lo) = if (la > lb) la to lb else lb to la
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(argb: Int): Double {
        val r = linearize((argb shr 16) and 0xFF)
        val g = linearize((argb shr 8) and 0xFF)
        val b = linearize(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun f(t: Double) = if (t > E) cbrt(t) else (K * t + 16) / 116
    private fun finv(ft: Double): Double {
        val t3 = ft * ft * ft
        return if (t3 > E) t3 else (116 * ft - 16) / K
    }

    private fun linearize(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun delinearize(v: Double): Int {
        val c = v.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
        return (s * 255).roundToInt().coerceIn(0, 255)
    }
}

/**
 * A hue and chroma, sampled at any tone. Where the requested chroma can't exist at a tone
 * (very light or dark), it backs off to the most colorful in-gamut color.
 */
class TonalPalette(val hue: Double, val chroma: Double) {
    private val cache = HashMap<Int, Int>()

    fun tone(t: Int): Int = cache.getOrPut(t) {
        when {
            t <= 0 -> ColorScience.argb(0, 0, 0)
            t >= 100 -> ColorScience.argb(255, 255, 255)
            else -> fit(t.toDouble())
        }
    }

    private fun fit(l: Double): Int {
        ColorScience.fromLch(l, chroma, hue)?.let { return it }
        var lo = 0.0
        var hi = chroma
        var best = ColorScience.fromLch(l, 0.0, hue) ?: grey(l)
        repeat(18) {
            val mid = (lo + hi) / 2
            val c = ColorScience.fromLch(l, mid, hue)
            if (c != null) {
                best = c
                lo = mid
            } else {
                hi = mid
            }
        }
        return best
    }

    private fun grey(l: Double): Int {
        val v = (l / 100 * 255).roundToInt().coerceIn(0, 255)
        return ColorScience.argb(v, v, v)
    }

    companion object {
        fun from(argb: Int, chroma: Double? = null): TonalPalette {
            val lch = ColorScience.toLch(argb)
            return TonalPalette(lch.h, chroma ?: lch.c)
        }
    }
}
