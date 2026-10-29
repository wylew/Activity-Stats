package com.wylew.activitystats.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.theme.ColorSource
import com.wylew.activitystats.theme.Presets
import com.wylew.activitystats.theme.ThemeMode
import com.wylew.activitystats.theme.ThemeSettings
import com.wylew.activitystats.ui.theme.colorSchemeFor

/**
 * Light / Dark / System, then either the wallpaper's Material You palette or two colors
 * the user picks. Edits preview live; the slider saves when you let go.
 */
@Composable
fun ThemeSection(theme: ThemeSettings, onChange: (ThemeSettings) -> Unit) {
    var draft by remember { mutableStateOf(theme) }
    LaunchedEffect(theme) { draft = theme }
    fun commit(t: ThemeSettings) {
        draft = t
        onChange(t)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("Theme")

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = draft.mode == m,
                    onClick = { commit(draft.copy(mode = m)) },
                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                ) { Text(m.label) }
            }
        }

        Column(Modifier.selectableGroup()) {
            SourceOption(
                title = "Match wallpaper",
                subtitle = "Material You colors pulled from your wallpaper",
                selected = draft.source == ColorSource.WALLPAPER,
                onClick = { commit(draft.copy(source = ColorSource.WALLPAPER)) },
            )
            SourceOption(
                title = "Pick your own colors",
                subtitle = "Choose two colors and get matching light and dark themes",
                selected = draft.source == ColorSource.CUSTOM,
                onClick = { commit(draft.copy(source = ColorSource.CUSTOM)) },
            )
        }

        if (draft.source == ColorSource.CUSTOM) {
            CustomColors(draft, onPreview = { draft = it }, onCommit = ::commit)
        }

        ThemePreview(draft)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SourceOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CustomColors(
    draft: ThemeSettings,
    onPreview: (ThemeSettings) -> Unit,
    onCommit: (ThemeSettings) -> Unit,
) {
    // 0 = primary slot, 1 = accent slot.
    var editing by remember { mutableIntStateOf(0) }
    val current = if (editing == 0) draft.primarySeed else draft.accentSeed
    var hue by remember(editing) { mutableFloatStateOf(Presets.hueOf(current)) }

    fun withColor(t: ThemeSettings, argb: Int) =
        if (editing == 0) t.copy(primarySeed = argb) else t.copy(accentSeed = argb)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel("Presets")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Presets.pairs) { pair ->
                val selected = draft.primarySeed == pair.primary && draft.accentSeed == pair.accent
                FilterChip(
                    selected = selected,
                    onClick = {
                        onCommit(draft.copy(primarySeed = pair.primary, accentSeed = pair.accent))
                        hue = Presets.hueOf(if (editing == 0) pair.primary else pair.accent)
                    },
                    label = { Text(pair.name) },
                    leadingIcon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Dot(pair.primary, 14)
                            Dot(pair.accent, 14)
                        }
                    },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorSlot("Primary", draft.primarySeed, editing == 0, Modifier.weight(1f)) { editing = 0 }
            ColorSlot("Accent", draft.accentSeed, editing == 1, Modifier.weight(1f)) { editing = 1 }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Presets.swatches.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { argb ->
                        val selected = argb == current
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Color(argb), if (selected) RoundedCornerShape(12.dp) else CircleShape)
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    onCommit(withColor(draft, argb))
                                    hue = Presets.hueOf(argb)
                                }
                                .semantics { contentDescription = "Color #%06X".format(argb and 0xFFFFFF) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Column {
            Text("Fine-tune hue", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    onPreview(withColor(draft, Presets.fromHue(it)))
                },
                onValueChangeFinished = { onCommit(withColor(draft, Presets.fromHue(hue))) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(Presets.fromHue(hue)),
                    activeTrackColor = Color(Presets.fromHue(hue)),
                ),
            )
        }
    }
}

@Composable
private fun ColorSlot(label: String, argb: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, RoundedCornerShape(20.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) cs.surfaceContainerHigh else cs.surface,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Dot(argb, 32)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text("#%06X".format(argb and 0xFFFFFF), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Dot(argb: Int, sizeDp: Int) {
    Box(
        Modifier
            .size(sizeDp.dp)
            .background(Color(argb), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

/** Light and dark samples of the scheme the user is building. */
@Composable
private fun ThemePreview(t: ThemeSettings) {
    val context = LocalContext.current
    val light = remember(t) { colorSchemeFor(context, t, dark = false) }
    val dark = remember(t) { colorSchemeFor(context, t, dark = true) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniPreview("Light", light, Modifier.weight(1f))
        MiniPreview("Dark", dark, Modifier.weight(1f))
    }
}

@Composable
private fun MiniPreview(label: String, cs: ColorScheme, modifier: Modifier) {
    Column(
        modifier
            .background(cs.surface, RoundedCornerShape(24.dp))
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(24.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Column(
            Modifier
                .fillMaxWidth()
                .background(cs.primary, RoundedCornerShape(18.dp))
                .padding(10.dp)
        ) {
            Text("13.2", color = cs.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.height(28.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                listOf(14, 20, 6, 24).forEachIndexed { i, h ->
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(h.dp)
                            .background(if (i == 3) cs.tertiaryContainer else cs.primaryContainer, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
        Text(
            "Accent",
            color = cs.onTertiaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(cs.tertiaryContainer, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(cs.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Text("Recent runs", color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
