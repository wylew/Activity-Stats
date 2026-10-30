package com.wylew.activitystats.ui.widgetconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Streak
import com.wylew.activitystats.domain.Streaks
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.ui.components.CookieShape
import com.wylew.activitystats.ui.components.RollupBars
import com.wylew.activitystats.widget.DotState
import com.wylew.activitystats.widget.Headline
import com.wylew.activitystats.widget.WidgetKind
import com.wylew.activitystats.widget.WidgetPalette
import com.wylew.activitystats.widget.WidgetPeriod
import com.wylew.activitystats.widget.WidgetSettings
import com.wylew.activitystats.widget.WidgetStyle
import com.wylew.activitystats.widget.chipName
import com.wylew.activitystats.widget.dotsFor
import com.wylew.activitystats.widget.headline
import com.wylew.activitystats.widget.iconFor
import com.wylew.activitystats.widget.statsLine
import com.wylew.activitystats.widget.streakCaption
import com.wylew.activitystats.widget.streakSub
import com.wylew.activitystats.widget.whenLabel
import com.wylew.activitystats.widget.widgetPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SQUARE = 176.dp
private val TALL = 184.dp

/** Compose mirror of a home-screen widget in the chosen style, at its natural size. */
@Composable
fun WidgetPreview(kind: WidgetKind, s: WidgetSettings, activities: List<Activity>, units: Units) {
    val p = MaterialTheme.colorScheme.widgetPalette(s.color)
    when (kind) {
        WidgetKind.TOTALS -> {
            val r = RollupCalculator.compute(activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
            val h = headline(r, s, units)
            when (s.style) {
                WidgetStyle.BADGE -> BadgeCard(p) {
                    val badgeText = if (h.big.length > 4) h.big.substringBefore('.') else h.big
                    BadgeHeader(badgeText, iconFor(s.filter), p)
                    Spacer(Modifier.weight(1f))
                    Text(h.caption, color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(h.second, color = p.sub, fontSize = 12.sp)
                    val dots = dotsFor(r)
                    if (dots.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        DotRow(dots, p)
                    }
                }
                WidgetStyle.NUMBER -> NumberCard(p, h, s)
                else -> BarsCard(p, h, s, r, units)
            }
        }
        WidgetKind.STREAK -> {
            val streak = Streaks.weekly(activities, s.filter, LocalDate.now(), ZoneId.systemDefault())
            when (s.style) {
                WidgetStyle.CALENDAR -> CalendarStreak(streak, s, p)
                WidgetStyle.NUMBER -> BadgeCard(p, padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewChip(chipName(s.filter), iconFor(s.filter), p)
                        Spacer(Modifier.weight(1f))
                        if (streak.thisWeekActive) Icon(painterResource(R.drawable.ic_flame), null, tint = p.current, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(streak.weeks.toString(), color = p.accent, fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold)
                    Text(streakCaption(streak), color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                else -> BadgeCard(p) {
                    BadgeHeader(streak.weeks.toString(), if (streak.thisWeekActive) R.drawable.ic_flame else iconFor(s.filter), p)
                    Spacer(Modifier.weight(1f))
                    Text(streakCaption(streak), color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(streakSub(streak, s), color = p.sub, fontSize = 11.sp, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    DotRow(streak.recent.map { if (it) DotState.ON else DotState.OFF }, p)
                }
            }
        }
        WidgetKind.LATEST -> {
            val latest = activities.firstOrNull { s.filter.matches(it.kind) }
            when (s.style) {
                WidgetStyle.CARD -> LatestCard(latest, s, units, p)
                WidgetStyle.COMPACT -> LatestCompact(latest, s, units, p)
                else -> LatestPill(latest, s, units, p)
            }
        }
    }
}

@Composable
private fun BadgeCard(p: WidgetPalette, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .size(SQUARE)
            .background(p.bg, RoundedCornerShape(28.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
private fun BadgeHeader(text: String, icon: Int, p: WidgetPalette) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.size(64.dp).background(p.accent, CookieShape()), contentAlignment = Alignment.Center) {
            Text(text, color = p.bg, fontSize = if (text.length > 3) 20.sp else 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.weight(1f))
        Icon(painterResource(icon), null, tint = p.fg, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DotRow(dots: List<DotState>, p: WidgetPalette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        dots.forEach { d ->
            Box(
                Modifier.weight(1f).height(10.dp).background(
                    when (d) {
                        DotState.ON -> p.fg
                        DotState.OFF -> p.future
                        DotState.FUTURE -> p.row
                    },
                    RoundedCornerShape(5.dp),
                )
            )
        }
    }
}

@Composable
private fun PreviewChip(text: String, icon: Int, p: WidgetPalette) {
    Row(
        Modifier.background(p.chipBg, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = p.chipFg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = p.chipFg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BigNumber(h: Headline, p: WidgetPalette, size: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            h.big, color = p.accent, fontSize = if (h.big.length > 4) (size * 0.75).sp else size.sp,
            lineHeight = size.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, maxLines = 1,
        )
        Spacer(Modifier.width(6.dp))
        Text(h.bigUnit, color = p.fg, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(bottom = 10.dp))
    }
}

@Composable
private fun BarsCard(p: WidgetPalette, h: Headline, s: WidgetSettings, r: Rollup, units: Units) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TALL)
            .background(p.bg, RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            PreviewChip(h.chip, iconFor(s.filter), p)
            Column {
                BigNumber(h, p, 56)
                Text(h.second, color = p.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            if (s.period != WidgetPeriod.DAILY) {
                RollupBars(r.buckets, barColor = p.bar, currentColor = p.current, futureColor = p.future, labelColor = p.sub, maxBarHeight = 108.dp)
            } else {
                val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (r.items.isEmpty()) Text("No ${s.filter.plural} yet today", color = p.sub, fontSize = 13.sp)
                    r.items.take(3).forEach { a ->
                        Row(Modifier.fillMaxWidth().background(p.row, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}", color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(Instant.ofEpochSecond(a.startEpochSec).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt), color = p.sub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberCard(p: WidgetPalette, h: Headline, s: WidgetSettings) {
    BadgeCard(p, padding = 18.dp) {
        PreviewChip(h.chip, iconFor(s.filter), p)
        Spacer(Modifier.weight(1f))
        BigNumber(h, p, 52)
        Text(h.second, color = p.sub, fontSize = 14.sp)
    }
}

@Composable
private fun CalendarStreak(streak: Streak, s: WidgetSettings, p: WidgetPalette) {
    BadgeCard(p, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(streak.weeks.toString(), color = p.accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text("weeks", color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(chipName(s.filter), color = p.sub, fontSize = 11.sp)
            }
            Icon(painterResource(iconFor(s.filter)), null, tint = p.fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            streak.recent.chunked(4).forEachIndexed { row, weeks ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    weeks.forEachIndexed { i, active ->
                        val thisWeek = row == 2 && i == 3
                        Box(
                            Modifier.weight(1f).height(18.dp).background(
                                if (active) (if (thisWeek) p.current else p.bar) else p.future,
                                RoundedCornerShape(if (thisWeek) 9.dp else 6.dp),
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(s: WidgetSettings, p: WidgetPalette, size: Dp) {
    Box(Modifier.size(size).background(p.chipBg, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(iconFor(s.filter)), null, tint = p.chipFg, modifier = Modifier.size(size / 2))
    }
}

@Composable
private fun LatestPill(latest: Activity?, s: WidgetSettings, units: Units, p: WidgetPalette) {
    Row(
        Modifier.fillMaxWidth().height(84.dp).background(p.bg, RoundedCornerShape(42.dp)).padding(start = 14.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(s, p, 56.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (latest == null) {
                Text("No ${s.filter.plural} yet", color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("${whenLabel(latest)} · ${latest.name}", color = p.sub, fontSize = 12.sp, maxLines = 1)
                Text(statsLine(latest, units), color = p.fg, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        latest?.origin?.let {
            Text(
                it, color = p.sub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(p.row, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LatestCard(latest: Activity?, s: WidgetSettings, units: Units, p: WidgetPalette) {
    BadgeCard(p) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(s, p, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(latest?.name ?: "No ${s.filter.plural} yet", color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (latest != null) Text(whenLabel(latest), color = p.sub, fontSize = 11.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        if (latest != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (latest.distanceM > 0) Format.distance(latest.distanceM, units) else Format.duration(latest.movingSec.toLong()),
                    color = p.accent, fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.width(4.dp))
                if (latest.distanceM > 0) Text(Format.distanceUnit(units), color = p.fg, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                CardStat("Time", Format.duration(latest.movingSec.toLong()), p, Modifier.weight(1f))
                if (latest.distanceM > 0) CardStat("Pace", Format.pace(latest.distanceM, latest.movingSec.toLong(), units), p, Modifier.weight(1f))
                latest.avgHr?.let { CardStat("HR", "%.0f".format(it), p, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CardStat(label: String, value: String, p: WidgetPalette, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = p.sub, fontSize = 11.sp)
        Text(value, color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LatestCompact(latest: Activity?, s: WidgetSettings, units: Units, p: WidgetPalette) {
    Row(
        Modifier.width(SQUARE).height(84.dp).background(p.bg, RoundedCornerShape(28.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(s, p, 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (latest == null) {
                Text("No ${s.filter.plural}", color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    if (latest.distanceM > 0) "${Format.distance(latest.distanceM, units)} ${Format.distanceUnit(units)}" else Format.duration(latest.movingSec.toLong()),
                    color = p.accent, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                )
                Text(whenLabel(latest).substringBefore(","), color = p.sub, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** Fills a preview area like a wallpaper, so widgets read as they will on the home screen. */
@Composable
fun WallpaperBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
