package com.example.data.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val durationMs: Long,
    val uri: Uri,
    val albumArtRes: Int? = null,
    val albumArtUri: Uri? = null,
    val lyrics: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val isBuiltIn: Boolean = false
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class VisualizerStyle {
    SPECTRUM_BARS,
    WAVE_NEON,
    CIRCULAR_PULSE,
    VINYL_RECORD
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class EqualizerBand(
    val id: Int,
    val label: String,
    val frequencyHz: Int,
    val gainDb: Int // -12 dB to +12 dB
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val selectedPreset: String = "Normal",
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, "60 Hz", 60, 3),
        EqualizerBand(1, "230 Hz", 230, 2),
        EqualizerBand(2, "910 Hz", 910, 0),
        EqualizerBand(3, "3.6 kHz", 3600, 2),
        EqualizerBand(4, "14 kHz", 14000, 4)
    ),
    val bassBoostStrength: Int = 30, // 0 to 100
    val virtualizerStrength: Int = 25 // 0 to 100
)
