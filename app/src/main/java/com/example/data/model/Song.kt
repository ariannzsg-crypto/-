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
    val filePath: String? = null,
    val lrcContent: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class VisualizerStyle {
    SPECTRUM_BARS,
    WAVE_NEON,
    RADIAL_PULSE
}
