package com.wylew.activitystats.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.graph
import com.wylew.activitystats.theme.ColorSource
import com.wylew.activitystats.theme.GeneratedScheme
import com.wylew.activitystats.theme.SchemeGenerator
import com.wylew.activitystats.theme.ThemeMode
import com.wylew.activitystats.theme.ThemeSettings

/**
 * Expressive-leaning type: heavy, tight display numbers for stats.
 * TODO: bundle Roboto Flex (res/font) and use its width axis for the condensed-wide stat look.
 */
private val base = Typography()
private val AppTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
)

/** Big stat numerals, used on cards and the widget preview. */
val StatNumeral = TextStyle(fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Theme driven by the user's Settings → Theme choices. */
@Composable
fun ActivityStatsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val flow = remember { context.graph.settings.theme }
    val settings by flow.collectAsState(initial = ThemeCache.last)
    SideEffect { ThemeCache.last = settings }
    ActivityStatsTheme(settings, content)
}

@Composable
fun ActivityStatsTheme(settings: ThemeSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(settings, dark) { colorSchemeFor(context, settings, dark) }

    // Status/nav bar icons follow the app's mode, not just the system's.
    val activity = context as? ComponentActivity
    LaunchedEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
    }

    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}

/** Last theme seen, so a second screen opens in the right colors without a flash. */
internal object ThemeCache {
    @Volatile var last: ThemeSettings = ThemeSettings()
}

/** Also used by the widgets, which aren't composed inside MaterialTheme. */
fun colorSchemeFor(context: Context, settings: ThemeSettings, dark: Boolean): ColorScheme = when (settings.source) {
    ColorSource.WALLPAPER -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    ColorSource.CUSTOM -> SchemeGenerator.generate(settings.primarySeed, settings.accentSeed, dark).toColorScheme(dark)
}

fun GeneratedScheme.toColorScheme(dark: Boolean): ColorScheme {
    fun c(v: Int) = Color(v)
    return if (dark) darkColorScheme(
        primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer),
        onPrimaryContainer = c(onPrimaryContainer), inversePrimary = c(inversePrimary),
        secondary = c(secondary), onSecondary = c(onSecondary), secondaryContainer = c(secondaryContainer),
        onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer),
        onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground),
        surface = c(surface), onSurface = c(onSurface), surfaceVariant = c(surfaceVariant),
        onSurfaceVariant = c(onSurfaceVariant), surfaceTint = c(surfaceTint),
        inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
        error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        outline = c(outline), outlineVariant = c(outlineVariant), scrim = c(scrim),
        surfaceBright = c(surfaceBright), surfaceContainer = c(surfaceContainer),
        surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
        surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
        surfaceDim = c(surfaceDim),
    ) else lightColorScheme(
        primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer),
        onPrimaryContainer = c(onPrimaryContainer), inversePrimary = c(inversePrimary),
        secondary = c(secondary), onSecondary = c(onSecondary), secondaryContainer = c(secondaryContainer),
        onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer),
        onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground),
        surface = c(surface), onSurface = c(onSurface), surfaceVariant = c(surfaceVariant),
        onSurfaceVariant = c(onSurfaceVariant), surfaceTint = c(surfaceTint),
        inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
        error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        outline = c(outline), outlineVariant = c(outlineVariant), scrim = c(scrim),
        surfaceBright = c(surfaceBright), surfaceContainer = c(surfaceContainer),
        surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
        surfaceContainerLow = c(surfaceContainerLow), surfaceContainerLowest = c(surfaceContainerLowest),
        surfaceDim = c(surfaceDim),
    )
}
