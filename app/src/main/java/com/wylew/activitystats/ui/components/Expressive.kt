package com.wylew.activitystats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.domain.Bucket
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Scalloped "cookie" shape in the spirit of M3 Expressive's shape library.
 * Swap for MaterialShapes.Cookie9Sided once those APIs are public in a stable material3.
 */
class CookieShape(private val lobes: Int = 9, private val depth: Float = 0.07f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) / (1f + depth)
        val steps = lobes * 24
        val path = Path()
        for (i in 0..steps) {
            val t = (2.0 * PI * i / steps)
            val rr = r * (1f + depth * cos(lobes * t).toFloat())
            val x = cx + rr * cos(t).toFloat()
            val y = cy + rr * sin(t).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}


/** Rounded pill bars; the current bucket is highlighted, future ones are outlined. */
@Composable
fun RollupBars(
    buckets: List<Bucket>,
    modifier: Modifier = Modifier,
    barColor: Color,
    currentColor: Color,
    futureColor: Color,
    labelColor: Color,
    maxBarHeight: Dp = 100.dp,
) {
    val max = buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { b ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                when {
                    b.isFuture -> Box(
                        Modifier
                            .widthIn(max = 28.dp)
                            .fillMaxWidth()
                            .height(20.dp)
                            .border(2.dp, futureColor, RoundedCornerShape(10.dp))
                    )
                    b.distanceM <= 0.0 -> Box(
                        Modifier
                            .size(8.dp)
                            .background(futureColor, RoundedCornerShape(4.dp))
                    )
                    else -> Box(
                        Modifier
                            .widthIn(max = 28.dp)
                            .fillMaxWidth()
                            .height(maxOf(10.dp, maxBarHeight * (b.distanceM / max).toFloat()))
                            .background(if (b.isCurrent) currentColor else barColor, RoundedCornerShape(14.dp))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    b.label,
                    color = labelColor,
                    fontSize = 11.sp,
                    fontWeight = if (b.isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                )
            }
        }
    }
}
