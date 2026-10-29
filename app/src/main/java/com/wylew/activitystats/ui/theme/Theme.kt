package com.wylew.activitystats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Fallback palette from the Rollup design, used when dynamic color is turned off.
private val Light = lightColorScheme(
    primary = Color(0xFF4459A9), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF00174D),
    secondaryContainer = Color(0xFFDDE1F9), onSecondaryContainer = Color(0xFF151B2C),
    tertiary = Color(0xFF8E4D2F), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC), onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFFBF8FF), surface = Color(0xFFFBF8FF),
    surfaceContainerLow = Color(0xFFF5F2FB), surfaceContainer = Color(0xFFEFEDF6),
    surfaceContainerHigh = Color(0xFFE9E7F0),
    onSurface = Color(0xFF1B1B21), onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680), outlineVariant = Color(0xFFC6C5D0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB5C4FF), onPrimary = Color(0xFF0F2878),
    primaryContainer = Color(0xFF2B4190), onPrimaryContainer = Color(0xFFDCE1FF),
    secondaryContainer = Color(0xFF41475A), onSecondaryContainer = Color(0xFFDDE1F9),
    tertiary = Color(0xFFFFB694), onTertiary = Color(0xFF552008),
    tertiaryContainer = Color(0xFF71361A), onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF121318), surface = Color(0xFF121318),
    surfaceContainerLow = Color(0xFF1B1B21), surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    onSurface = Color(0xFFE4E1E9), onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A), outlineVariant = Color(0xFF45464F),
)

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

@Composable
fun ActivityStatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
}
