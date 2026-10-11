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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerScreen(
    enabled: Boolean,
    presets: List<String>,
    currentPreset: String,
    bandFrequencies: List<Int>,
    bandLevels: Map<Short, Short>,
    bassBoost: Int,
    virtualizer: Int,
    onPresetSelected: (String) -> Unit,
    onBandLevelChange: (Short, Short) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("equalizer_screen")
    ) {
        item {
            Text(
                text = "اکولایزر گرافیکی و تقویت صدا",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Presets Chips
        item {
            Text(text = "پریست‌های صوتی:", color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val displayPresets = if (presets.isNotEmpty()) presets else listOf("Flat", "Bass", "Rock", "Pop", "Jazz", "Electronic")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayPresets) { preset ->
                    val isSelected = preset == currentPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonViolet else SurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else Color(0x22FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onPresetSelected(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 5-Band Equalizer Sliders
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "تنظیم فرکانس‌ها (Graphic EQ)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val bands = if (bandFrequencies.isNotEmpty()) bandFrequencies else listOf(60, 230, 910, 3600, 14000)
                    bands.forEachIndexed { index, freq ->
                        val band = index.toShort()
                        val currentLevel = bandLevels[band] ?: 0
                        // Level range is typically -1500 to +1500 millibels (-15dB to +15dB)
                        val sliderValue = ((currentLevel.toFloat() + 1500f) / 3000f).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.25f)
                            )

                            Slider(
                                value = sliderValue,
                                onValueChange = { frac ->
                                    val newLevel = ((frac * 3000f) - 1500f).toInt().toShort()
                                    onBandLevelChange(band, newLevel)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonViolet,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.weight(0.6f)
                            )

                            Text(
                                text = "%+d dB".format(currentLevel / 100),
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.15f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bass Boost Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "تقویت بیس (Bass Boost)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "${bassBoost / 10}%", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = (bassBoost / 1000f).coerceIn(0f, 1f),
                        onValueChange = { onBassBoostChange((it * 1000).toInt()) },
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonPink,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3D Surround Virtualizer Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "صدای سه‌بعدی (3D Surround)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "${virtualizer / 10}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = (virtualizer / 1000f).coerceIn(0f, 1f),
                        onValueChange = { onVirtualizerChange((it * 1000).toInt()) },
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
