package com.example.data.lyrics

import com.example.data.model.LyricLine
import java.io.File
import java.util.regex.Pattern

object LrcParser {

    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")

    /**
     * Parses an LRC formatted string into an ordered list of LyricLine objects.
     */
    fun parse(lrcText: String?): List<LyricLine> {
        if (lrcText.isNullOrBlank()) return emptyList()

        val lines = lrcText.lines()
        val result = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Skip metadata tags like [ti:Title], [ar:Artist], etc.
            if (trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") ||
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") ||
                trimmed.startsWith("[offset:")
            ) {
                continue
            }

            val matcher = TIME_TAG_PATTERN.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val fracStr = matcher.group(3) ?: "0"

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L

                val millis = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }

                val totalMs = minutes * 60 * 1000 + seconds * 1000 + millis
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricText = trimmed.substring(lastMatchEnd).trim()
                for (ts in timestamps) {
                    result.add(LyricLine(timestampMs = ts, text = lyricText))
                }
            } else if (result.isEmpty()) {
                // If the file is plain text without LRC tags, fallback to evenly spaced lines
                // handled by parsePlainText below
            }
        }

        return if (result.isNotEmpty()) {
            result.sortedBy { it.timestampMs }
        } else {
            parsePlainText(lrcText)
        }
    }

    /**
     * Fallback for plain text lyrics without timestamps (spaces them 3 seconds apart).
     */
    fun parsePlainText(plainText: String, lineIntervalMs: Long = 3000L): List<LyricLine> {
        val lines = plainText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return lines.mapIndexed { index, text ->
            LyricLine(timestampMs = index * lineIntervalMs, text = text)
        }
    }

    /**
     * Finds the index of the currently active lyric line for the given playback time.
     */
    fun findActiveIndex(lyrics: List<LyricLine>, currentPositionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        if (currentPositionMs < lyrics.first().timestampMs) return 0

        var low = 0
        var high = lyrics.size - 1
        var candidate = 0

        while (low <= high) {
            val mid = (low + high) / 2
            if (lyrics[mid].timestampMs <= currentPositionMs) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return candidate
    }

    /**
     * Checks if a companion .lrc file exists for a given audio file path.
     * E.g., for /storage/emulated/0/Music/Song.mp3 -> /storage/emulated/0/Music/Song.lrc
     */
    fun findCompanionLrcFile(audioFilePath: String?): File? {
        if (audioFilePath.isNullOrBlank()) return null
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) return null

        val nameWithoutExt = audioFile.nameWithoutExtension
        val parentDir = audioFile.parentFile ?: return null

        val candidate1 = File(parentDir, "$nameWithoutExt.lrc")
        if (candidate1.exists() && candidate1.isFile) return candidate1

        val candidate2 = File(parentDir, "$nameWithoutExt.LRC")
        if (candidate2.exists() && candidate2.isFile) return candidate2

        return null
    }

    /**
     * Reads companion LRC file content if present.
     */
    fun readCompanionLrc(audioFilePath: String?): String? {
        return try {
            val file = findCompanionLrcFile(audioFilePath)
            if (file != null && file.canRead()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
