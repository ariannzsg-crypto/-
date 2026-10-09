package com.example.data.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object WavAudioGenerator {

    fun generateSampleTrack(context: Context, fileName: String, trackType: Int): File {
        val file = File(context.cacheDir, fileName)
        if (file.exists() && file.length() > 1000) {
            return file
        }

        val sampleRate = 44100
        val durationSeconds = 18 // 18 seconds loop
        val totalSamples = sampleRate * durationSeconds
        val numChannels = 2
        val bytesPerSample = 2
        val dataSize = totalSamples * numChannels * bytesPerSample

        val buffer = ByteBuffer.allocate(44 + dataSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())

        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(numChannels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
        buffer.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample

        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        // Generate musical audio waveform samples
        when (trackType) {
            1 -> generateSynthwave(buffer, totalSamples, sampleRate)
            2 -> generatePersianMelody(buffer, totalSamples, sampleRate)
            3 -> generateLofiChill(buffer, totalSamples, sampleRate)
            else -> generateElectroBeat(buffer, totalSamples, sampleRate)
        }

        FileOutputStream(file).use { fos ->
            fos.write(buffer.array())
        }

        return file
    }

    private fun generateSynthwave(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val bpm = 120.0
        val beatDuration = 60.0 / bpm
        // C minor scale frequencies: C4(261.63), Eb4(311.13), G4(392.00), Bb4(466.16), C5(523.25)
        val arpNotes = doubleArrayOf(261.63, 311.13, 392.00, 523.25, 466.16, 392.00, 311.13, 392.00)
        val bassFreq = 65.41 // C2

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val currentBeat = t / beatDuration
            val noteIndex = (currentBeat * 4).toInt() % arpNotes.size
            val leadFreq = arpNotes[noteIndex]

            // Arp Synth
            val leadWave = sin(2.0 * PI * leadFreq * t) * 0.35 +
                    sin(4.0 * PI * leadFreq * t) * 0.1

            // Bass synth (pulsing 8th notes)
            val bassEnv = 1.0 - ((currentBeat * 2) % 1.0)
            val bassWave = (sin(2.0 * PI * bassFreq * t) + sin(4.0 * PI * bassFreq * t) * 0.3) * (0.35 * bassEnv)

            // Kick drum on every beat
            val beatFrac = currentBeat % 1.0
            val kickFreq = 140.0 * (1.0 - beatFrac).coerceAtLeast(0.0) + 45.0
            val kickEnv = (1.0 - beatFrac * 3.0).coerceAtLeast(0.0)
            val kick = sin(2.0 * PI * kickFreq * beatFrac * beatDuration) * kickEnv * 0.4

            // Pad background
            val pad = (sin(2.0 * PI * 130.81 * t) + sin(2.0 * PI * 196.00 * t)) * 0.12

            val left = ((leadWave * 0.8 + bassWave + kick + pad) * 28000).toInt().coerceIn(-32767, 32767).toShort()
            val right = ((leadWave * 1.0 + bassWave + kick + pad) * 28000).toInt().coerceIn(-32767, 32767).toShort()

            buffer.putShort(left)
            buffer.putShort(right)
        }
    }

    private fun generatePersianMelody(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val bpm = 90.0
        val beatDuration = 60.0 / bpm
        // Mystical Persian Shoushtari scale notes around D (D4=293.66, Eb4=311.13, F#4=369.99, G4=392.00, A4=440.00)
        val melodyNotes = doubleArrayOf(293.66, 311.13, 369.99, 392.00, 369.99, 311.13, 293.66, 220.00)
        val droneD = 146.83 // D3 Drone

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val currentBeat = t / beatDuration
            val noteIndex = (currentBeat * 2).toInt() % melodyNotes.size
            val melFreq = melodyNotes[noteIndex]

            // Tar / Setar pluck simulation with decay and vibrato
            val noteFrac = (currentBeat * 2) % 1.0
            val vibrato = sin(2.0 * PI * 5.0 * t) * 3.0
            val pluckEnv = Math.exp(-noteFrac * 3.5)
            val pluck = (sin(2.0 * PI * (melFreq + vibrato) * t) +
                    0.5 * sin(4.0 * PI * melFreq * t) +
                    0.25 * sin(6.0 * PI * melFreq * t)) * pluckEnv * 0.45

            // Drone string
            val drone = (sin(2.0 * PI * droneD * t) + sin(2.0 * PI * (droneD * 1.5) * t) * 0.5) * 0.2

            // Daf / Tombak rhythm hit on beats 1 and 3
            val barFrac = currentBeat % 2.0
            val tombakEnv = (1.0 - (barFrac % 1.0) * 4.0).coerceAtLeast(0.0)
            val tombak = sin(2.0 * PI * 85.0 * t) * tombakEnv * 0.3

            val left = ((pluck * 0.9 + drone + tombak) * 26000).toInt().coerceIn(-32767, 32767).toShort()
            val right = ((pluck * 1.1 + drone + tombak) * 26000).toInt().coerceIn(-32767, 32767).toShort()

            buffer.putShort(left)
            buffer.putShort(right)
        }
    }

    private fun generateLofiChill(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val bpm = 75.0
        val beatDuration = 60.0 / bpm
        // Warm jazz chord progression Dm9 -> G13 -> Cmaj7
        val chordFreqs = arrayOf(
            doubleArrayOf(293.66, 349.23, 440.00, 523.25), // Dm9
            doubleArrayOf(196.00, 246.94, 329.63, 440.00), // G13
            doubleArrayOf(261.63, 329.63, 392.00, 493.88)  // Cmaj7
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val currentBeat = t / beatDuration
            val chordIndex = ((currentBeat / 4).toInt()) % chordFreqs.size
            val freqs = chordFreqs[chordIndex]

            // Electric Piano (Rhodes) mellow warm sine blend
            var rhodes = 0.0
            for (f in freqs) {
                rhodes += sin(2.0 * PI * f * t) * 0.1
            }

            // Lo-Fi vinyl crackle / tape flutter
            val crackle = (Math.random() - 0.5) * 0.02

            // Sub bass
            val subBass = sin(2.0 * PI * 73.42 * t) * 0.25

            // Chill snare on beats 2 and 4
            val snareBeat = (currentBeat % 2.0)
            val snareEnv = if (snareBeat in 0.9..1.1) (1.0 - Math.abs(snareBeat - 1.0) * 8.0).coerceAtLeast(0.0) else 0.0
            val snare = (Math.random() - 0.5) * snareEnv * 0.2

            val left = ((rhodes + subBass + snare + crackle) * 26000).toInt().coerceIn(-32767, 32767).toShort()
            val right = ((rhodes + subBass + snare + crackle) * 26000).toInt().coerceIn(-32767, 32767).toShort()

            buffer.putShort(left)
            buffer.putShort(right)
        }
    }

    private fun generateElectroBeat(buffer: ByteBuffer, totalSamples: Int, sampleRate: Int) {
        val bpm = 128.0
        val beatDuration = 60.0 / bpm
        val arp = doubleArrayOf(330.0, 392.0, 493.88, 659.25, 493.88, 392.0)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val currentBeat = t / beatDuration
            val noteIdx = (currentBeat * 4).toInt() % arp.size
            val freq = arp[noteIdx]

            val lead = sin(2.0 * PI * freq * t) * 0.3
            val bass = sin(2.0 * PI * 82.41 * t) * 0.35

            // Punchy Kick
            val beatFrac = currentBeat % 1.0
            val kickEnv = (1.0 - beatFrac * 4.0).coerceAtLeast(0.0)
            val kick = sin(2.0 * PI * (120.0 * (1.0 - beatFrac) + 50.0) * beatFrac * beatDuration) * kickEnv * 0.45

            val left = ((lead + bass + kick) * 27000).toInt().coerceIn(-32767, 32767).toShort()
            val right = ((lead * 1.1 + bass + kick) * 27000).toInt().coerceIn(-32767, 32767).toShort()

            buffer.putShort(left)
            buffer.putShort(right)
        }
    }
}
