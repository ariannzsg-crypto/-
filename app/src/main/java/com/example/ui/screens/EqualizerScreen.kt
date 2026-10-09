package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

val PRESET_OPTIONS = mapOf(
    "Flat" to listOf(0, 0, 0, 0, 0),
    "Bass Boost" to listOf(9, 6, 2, 0, 1),
    "Rock" to listOf(6, 4, -1, 4, 7),
    "Pop" to listOf(3, 5, 6, 4, 2),
    "Jazz" to listOf(4, 3, 1, 4, 5),
    "Electronic" to listOf(7, 5, 0, 5, 8),
    "Vocal" to listOf(-2, 1, 6, 4, -1),
    "Classical" to listOf(5, 3, -1, 3, 5),
    "Hip-Hop" to listOf(8, 6, 1, 3, 4)
)

@Composable
fun EqualizerScreen(
    state: EqualizerState,
    onUpdateEqualizer: (EqualizerState) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Equalizer Header & Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonViolet.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "اکولایزر صوتی (Equalizer)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (state.isEnabled) "فعال • جلوه‌های صوتی روشن" else "غیرفعال",
                            color = if (state.isEnabled) NeonCyan else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val flatBands = state.bands.map { it.copy(gainDb = 0) }
                            onUpdateEqualizer(state.copy(selectedPreset = "Flat", bands = flatBands, bassBoostStrength = 0, virtualizerStrength = 0))
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset EQ", tint = TextMuted)
                    }

                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { onUpdateEqualizer(state.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonViolet,
                            uncheckedTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("eq_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Chips
        Text(
            text = "حالت‌های پیش‌فرض (Presets)",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PRESET_OPTIONS.keys.toList()) { preset ->
                val isSel = state.selectedPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) NeonCyan else SurfaceCard)
                        .border(1.dp, if (isSel) NeonCyan else SurfaceHighlight, RoundedCornerShape(14.dp))
                        .clickable(enabled = state.isEnabled) {
                            val gains = PRESET_OPTIONS[preset] ?: listOf(0, 0, 0, 0, 0)
                            val newBands = state.bands.mapIndexed { i, band ->
                                band.copy(gainDb = gains.getOrElse(i) { 0 })
                            }
                            onUpdateEqualizer(state.copy(selectedPreset = preset, bands = newBands))
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset,
                        color = if (isSel) Color.Black else TextPrimary,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5-Band Equalizer Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "تنظیمات فرکانس (Graphic 5-Band EQ)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                state.bands.forEachIndexed { index, band ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = band.label,
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(60.dp)
                        )

                        Slider(
                            value = band.gainDb.toFloat(),
                            onValueChange = { newVal ->
                                val newGain = newVal.toInt()
                                val updatedBands = state.bands.toMutableList()
                                updatedBands[index] = band.copy(gainDb = newGain)
                                onUpdateEqualizer(state.copy(selectedPreset = "Custom", bands = updatedBands))
                            },
                            valueRange = -12f..12f,
                            steps = 23,
                            enabled = state.isEnabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonViolet,
                                activeTrackColor = NeonViolet,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )

                        Text(
                            text = if (band.gainDb > 0) "+${band.gainDb} dB" else "${band.gainDb} dB",
                            color = if (band.gainDb > 0) NeonPink else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(55.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bass Boost Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تقویت بیس (Bass Boost)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "${state.bassBoostStrength}%",
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Slider(
                    value = state.bassBoostStrength.toFloat(),
                    onValueChange = { onUpdateEqualizer(state.copy(bassBoostStrength = it.toInt())) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPink,
                        activeTrackColor = NeonPink,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3D Surround / Virtualizer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SurroundSound, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "صدای فراگیر سه‌بعدی (3D Surround)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "${state.virtualizerStrength}%",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Slider(
                    value = state.virtualizerStrength.toFloat(),
                    onValueChange = { onUpdateEqualizer(state.copy(virtualizerStrength = it.toInt())) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
