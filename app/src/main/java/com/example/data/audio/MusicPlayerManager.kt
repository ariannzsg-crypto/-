package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    // Equalizer state
    private val _equalizerEnabled = MutableStateFlow(true)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPresets = MutableStateFlow<List<String>>(emptyList())
    val equalizerPresets: StateFlow<List<String>> = _equalizerPresets.asStateFlow()

    private val _currentPreset = MutableStateFlow("Normal")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(500) // 0 - 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(300) // 0 - 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _bandFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Short, Short>>(emptyMap())
    val bandLevels: StateFlow<Map<Short, Short>> = _bandLevels.asStateFlow()

    var onSongFinished: ((Song) -> Unit)? = null

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = false) {
        _queue.value = songs
        if (songs.isNotEmpty()) {
            val safeIndex = startIndex.coerceIn(0, songs.size - 1)
            val song = songs[safeIndex]
            _currentSong.value = song
            _durationMs.value = song.durationMs
            _currentPositionMs.value = 0L
            if (autoPlay) {
                playSong(song, true)
            } else {
                _isPlaying.value = false
            }
        }
    }

    fun playSong(song: Song, startPlaying: Boolean = true) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Throwable) { }
        mediaPlayer = null

        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Throwable) { }
        equalizer = null
        bassBoost = null
        virtualizer = null

        _currentSong.value = song
        _durationMs.value = song.durationMs
        _currentPositionMs.value = 0L

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, song.uri)
                prepare()
            }
            mediaPlayer = player

            try {
                initAudioEffects(player.audioSessionId)
            } catch (e: Throwable) {
                Log.w("MusicPlayerManager", "Audio effects init skipped", e)
            }

            player.setOnCompletionListener {
                handleSongCompletion()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerManager", "MediaPlayer error: $what, $extra")
                _isPlaying.value = false
                stopProgressTracker()
                true
            }

            val realDuration = try { player.duration.toLong() } catch (e: Throwable) { song.durationMs }
            if (realDuration > 0) {
                _durationMs.value = realDuration
            }

            if (startPlaying) {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            } else {
                _isPlaying.value = false
            }
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Failed to play song: ${song.title}", e)
            _isPlaying.value = false
            stopProgressTracker()
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerEnabled.value
            }

            val presets = mutableListOf<String>()
            val numPresets = equalizer?.numberOfPresets ?: 0
            for (i in 0 until numPresets) {
                try {
                    equalizer?.getPresetName(i.toShort())?.let { presets.add(it) }
                } catch (e: Throwable) { }
            }
            if (presets.isEmpty()) {
                presets.addAll(listOf("Flat", "Bass", "Rock", "Pop", "Jazz", "Electronic", "Classical"))
            }
            _equalizerPresets.value = presets

            val numBands = equalizer?.numberOfBands ?: 0
            val freqs = mutableListOf<Int>()
            val levels = mutableMapOf<Short, Short>()
            for (i in 0 until numBands) {
                val band = i.toShort()
                val centerFreq = try { equalizer?.getCenterFreq(band)?.div(1000) ?: ((i + 1) * 200) } catch (e: Throwable) { (i + 1) * 200 }
                freqs.add(centerFreq)
                levels[band] = try { equalizer?.getBandLevel(band) ?: 0 } catch (e: Throwable) { 0 }
            }
            _bandFrequencies.value = freqs
            _bandLevels.value = levels
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Equalizer unavailable", e)
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
                if (strengthSupported) setStrength(_bassBoostStrength.value.toShort())
            }
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "BassBoost unavailable", e)
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
                if (strengthSupported) setStrength(_virtualizerStrength.value.toShort())
            }
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Virtualizer unavailable", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null) {
            val cur = _currentSong.value
            if (cur != null) {
                playSong(cur, true)
            }
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "togglePlayPause failed", e)
            val cur = _currentSong.value
            if (cur != null) playSong(cur, true)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
    }

    fun playNext() {
        val q = _queue.value
        val cur = _currentSong.value ?: return
        if (q.isEmpty()) return

        val curIndex = q.indexOfFirst { it.id == cur.id }
        val nextIndex = when {
            _isShuffle.value -> q.indices.random()
            curIndex in 0 until q.size - 1 -> curIndex + 1
            _repeatMode.value == RepeatMode.ALL -> 0
            else -> null
        }

        if (nextIndex != null) {
            playSong(q[nextIndex], true)
        } else {
            mediaPlayer?.pause()
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }
    }

    fun playPrevious() {
        val q = _queue.value
        val cur = _currentSong.value ?: return
        if (q.isEmpty()) return

        // If played more than 3 seconds, restart current song
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val curIndex = q.indexOfFirst { it.id == cur.id }
        val prevIndex = when {
            curIndex > 0 -> curIndex - 1
            _repeatMode.value == RepeatMode.ALL -> q.size - 1
            else -> 0
        }
        playSong(q[prevIndex], true)
    }

    private fun handleSongCompletion() {
        val cur = _currentSong.value
        if (cur != null) onSongFinished?.invoke(cur)

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            else -> playNext()
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()

        if (minutes == null || minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        var totalSeconds = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            while (totalSeconds > 0 && isActive) {
                delay(1000L)
                totalSeconds--
                _sleepTimerRemainingSeconds.value = totalSeconds
            }
            if (isActive) {
                // Pause playback when timer completes
                mediaPlayer?.pause()
                _isPlaying.value = false
                _sleepTimerMinutes.value = null
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun setEqualizerPreset(presetName: String) {
        _currentPreset.value = presetName
        try {
            val presets = _equalizerPresets.value
            val idx = presets.indexOf(presetName)
            if (idx >= 0) {
                equalizer?.usePreset(idx.toShort())
                // update band levels
                val levels = mutableMapOf<Short, Short>()
                val bands = equalizer?.numberOfBands ?: 0
                for (b in 0 until bands) {
                    val band = b.toShort()
                    levels[band] = equalizer?.getBandLevel(band) ?: 0
                }
                _bandLevels.value = levels
            }
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Failed to apply preset $presetName", e)
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            val updated = _bandLevels.value.toMutableMap()
            updated[band] = level
            _bandLevels.value = updated
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Failed to set band level", e)
        }
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength
        try {
            bassBoost?.setStrength(strength.toShort())
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Failed to set bass boost", e)
        }
    }

    fun setVirtualizer(strength: Int) {
        _virtualizerStrength.value = strength
        try {
            virtualizer?.setStrength(strength.toShort())
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Failed to set virtualizer", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                _currentPositionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(100L) // Smooth 100ms updates for precise lyrics sync
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracker()
        sleepTimerJob?.cancel()
        try {
            mediaPlayer?.release()
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Error in release", e)
        }
    }
}
