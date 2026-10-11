package com.example.data.audio

import android.content.Context
import com.example.R
import com.example.data.model.Song
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object WavAudioGenerator {

    private const val SAMPLE_RATE = 22050
    private const val DURATION_SECONDS = 15

    fun getOrGenerateDemoSongs(context: Context): List<Song> {
        val tracksDir = File(context.filesDir, "demo_tracks")
        if (!tracksDir.exists()) tracksDir.mkdirs()

        val track1File = File(tracksDir, "neon_horizon.wav")
        if (!track1File.exists() || track1File.length() == 0L) {
            generateSynthwaveTrack(track1File)
        }

        val track2File = File(tracksDir, "ava_shoushtari.wav")
        if (!track2File.exists() || track2File.length() == 0L) {
            generatePersianTarTrack(track2File)
        }

        val track3File = File(tracksDir, "midnight_lofi.wav")
        if (!track3File.exists() || track3File.length() == 0L) {
            generateLofiTrack(track3File)
        }

        return listOf(
            Song(
                id = "demo_neon_horizon",
                title = "Neon Horizon",
                artist = "Cyber Pulse",
                album = "Retrowave 2077",
                durationMs = DURATION_SECONDS * 1000L,
                uri = android.net.Uri.fromFile(track1File),
                albumArtRes = R.drawable.album_art_neon,
                filePath = track1File.absolutePath,
                lrcContent = """
                    [00:00.00]♪ Cyber Synth Intro ♪
                    [00:02.50]Racing through the neon glowing lights
                    [00:05.00]Electric purple city in the night
                    [00:08.00]Digital heartbeat pulses through the wire
                    [00:11.50]Synthesizer burning like a cosmic fire
                    [00:14.00]Outrun the shadow into the cyber dawn ♪
                """.trimIndent(),
                isFavorite = true
            ),
            Song(
                id = "demo_ava_shoushtari",
                title = "نوای شوشتری (Shoushtari)",
                artist = "استاد تار و سه تار",
                album = "آواز کهن ایران",
                durationMs = DURATION_SECONDS * 1000L,
                uri = android.net.Uri.fromFile(track2File),
                albumArtRes = R.drawable.album_art_persian,
                filePath = track2File.absolutePath,
                lrcContent = """
                    [00:00.00]مقدمه دلنشین گوشه شوشتری
                    [00:02.50]زخمه‌های تار در سکوت شبانگاه
                    [00:06.00]ای دل تو مرو سوی پریشانی راز
                    [00:09.50]نغمه‌ای خوش زن و با باد صبا ساز نواز
                    [00:12.50]طنین تار بر گنبد مینای آسمان ♪
                """.trimIndent(),
                isFavorite = true
            ),
            Song(
                id = "demo_midnight_lofi",
                title = "Midnight Rain Coffee",
                artist = "Lofi Study Beats",
                album = "Rainy Window EP",
                durationMs = DURATION_SECONDS * 1000L,
                uri = android.net.Uri.fromFile(track3File),
                albumArtRes = R.drawable.album_art_chill,
                filePath = track3File.absolutePath,
                lrcContent = """
                    [00:00.00]Raindrops tapping softly on the window
                    [00:03.00]Warm coffee aroma filling the quiet room
                    [00:07.00]Lofi chill chords soothing your mind
                    [00:10.50]Deep breath in, letting time gently unwind
                    [00:13.50]Peaceful night, peaceful thoughts ♪
                """.trimIndent(),
                isFavorite = false
            )
        )
    }

    private fun generateSynthwaveTrack(outputFile: File) {
        val totalSamples = SAMPLE_RATE * DURATION_SECONDS
        val buffer = ShortArray(totalSamples)

        val chordFreqs = doubleArrayOf(220.0, 261.63, 329.63, 392.0) // Am7
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val bass = sin(2 * PI * 55.0 * t) * 0.4
            val chordIndex = (t * 2).toInt() % chordFreqs.size
            val lead = sin(2 * PI * chordFreqs[chordIndex] * t) * 0.3
            val sample = (bass + lead) * 32767.0 * 0.6
            buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        writeWav(outputFile, buffer, SAMPLE_RATE)
    }

    private fun generatePersianTarTrack(outputFile: File) {
        val totalSamples = SAMPLE_RATE * DURATION_SECONDS
        val buffer = ShortArray(totalSamples)

        // Shoushtari scale notes approx: D4, Eb4, F#4, G4, A4
        val notes = doubleArrayOf(293.66, 311.13, 369.99, 392.0, 440.0)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = ((t * 1.5).toInt()) % notes.size
            val freq = notes[noteIdx]
            val decay = 1.0 - ((t * 1.5) % 1.0) * 0.3
            val wave = (sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * freq * 2 * t)) * decay * 0.5
            val drone = sin(2 * PI * 146.83 * t) * 0.2 // D3 drone
            val sample = (wave + drone) * 32767.0 * 0.6
            buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        writeWav(outputFile, buffer, SAMPLE_RATE)
    }

    private fun generateLofiTrack(outputFile: File) {
        val totalSamples = SAMPLE_RATE * DURATION_SECONDS
        val buffer = ShortArray(totalSamples)

        val chords = arrayOf(
            doubleArrayOf(174.61, 220.0, 261.63, 329.63), // Fmaj7
            doubleArrayOf(164.81, 196.0, 246.94, 293.66), // Em7
            doubleArrayOf(146.83, 174.61, 220.0, 261.63)  // Dm7
        )
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val chordIdx = ((t / 2.0).toInt()) % chords.size
            val currentChord = chords[chordIdx]
            var sum = 0.0
            for (f in currentChord) {
                sum += sin(2 * PI * f * t)
            }
            sum /= currentChord.size
            val sub = sin(2 * PI * 43.65 * t) * 0.4
            val sample = (sum * 0.5 + sub) * 32767.0 * 0.6
            buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        writeWav(outputFile, buffer, SAMPLE_RATE)
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val byteRate = sampleRate * 2 // 16-bit mono
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size
            putShort(1) // AudioFormat PCM
            putShort(1) // NumChannels 1 (Mono)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray())
            putInt(dataSize)
        }

        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val sampleBuffer = ByteBuffer.allocate(samples.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (s in samples) putShort(s)
            }
            fos.write(sampleBuffer.array())
        }
    }
}
