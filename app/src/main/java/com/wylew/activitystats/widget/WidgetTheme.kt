package com.wylew.activitystats.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.wylew.activitystats.theme.ThemeMode
import com.wylew.activitystats.theme.ThemeSettings
import com.wylew.activitystats.ui.theme.colorSchemeFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.glance.color.ColorProvider as DayNightColor

/** One widget's colors. Shared by the Glance widgets and the in-app Compose previews. */
data class WidgetPalette(
    val bg: Color,
    val fg: Color,
    val sub: Color,
    val accent: Color,
    val bar: Color,
    val current: Color,
    val future: Color,
    val chipBg: Color,
    val chipFg: Color,
    val row: Color,
)

fun ColorScheme.widgetPalette(color: WidgetColor): WidgetPalette = when (color) {
    WidgetColor.NEUTRAL -> WidgetPalette(
        bg = surfaceContainerLow, fg = onSurface, sub = onSurfaceVariant, accent = primary,
        bar = primary, current = tertiary, future = outlineVariant,
        chipBg = primaryContainer, chipFg = onPrimaryContainer, row = surfaceContainerHigh,
    )
    WidgetColor.PRIMARY -> WidgetPalette(
        bg = primary, fg = onPrimary, sub = onPrimary.copy(alpha = 0.85f), accent = onPrimary,
        bar = primaryContainer, current = tertiaryContainer, future = onPrimary.copy(alpha = 0.32f),
        chipBg = primaryContainer, chipFg = onPrimaryContainer, row = onPrimary.copy(alpha = 0.16f),
    )
    WidgetColor.TERTIARY -> WidgetPalette(
        bg = tertiaryContainer, fg = onTertiaryContainer, sub = onTertiaryContainer.copy(alpha = 0.85f), accent = tertiary,
        bar = tertiary, current = primary, future = tertiary.copy(alpha = 0.28f),
        chipBg = tertiary, chipFg = onTertiary, row = tertiary.copy(alpha = 0.14f),
    )
}

/** The same palette as Glance color providers, following light/dark like the app does. */
class GlancePalette(
    val bg: ColorProvider,
    val fg: ColorProvider,
    val sub: ColorProvider,
    val accent: ColorProvider,
    val bar: ColorProvider,
    val current: ColorProvider,
    val future: ColorProvider,
    val chipBg: ColorProvider,
    val chipFg: ColorProvider,
    val row: ColorProvider,
)

fun glancePalette(context: Context, theme: ThemeSettings, color: WidgetColor): GlancePalette {
    val light = colorSchemeFor(context, theme, dark = false).widgetPalette(color)
    val dark = colorSchemeFor(context, theme, dark = true).widgetPalette(color)
    fun p(pick: (WidgetPalette) -> Color): ColorProvider = when (theme.mode) {
        ThemeMode.SYSTEM -> DayNightColor(day = pick(light), night = pick(dark))
        ThemeMode.LIGHT -> ColorProvider(pick(light))
        ThemeMode.DARK -> ColorProvider(pick(dark))
    }
    return GlancePalette(
        bg = p { it.bg }, fg = p { it.fg }, sub = p { it.sub }, accent = p { it.accent },
        bar = p { it.bar }, current = p { it.current }, future = p { it.future },
        chipBg = p { it.chipBg }, chipFg = p { it.chipFg }, row = p { it.row },
    )
}

/**
 * Shapes Glance can't draw, rendered as white bitmaps and tinted at display time,
 * so they still follow light/dark without re-rendering.
 */
object WidgetBitmaps {
    /** Nine-lobed "cookie" badge. */
    fun cookie(sizePx: Int, lobes: Int = 9, depth: Float = 0.07f): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = sizePx / 2f
        val r = min(c, c) / (1f + depth)
        val path = Path()
        val steps = lobes * 24
        for (i in 0..steps) {
            val t = 2 * PI * i / steps
            val rr = r * (1f + depth * cos(lobes * t).toFloat())
            val x = c + rr * cos(t).toFloat()
            val y = c + rr * sin(t).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        Canvas(bmp).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
        return bmp
    }
}
