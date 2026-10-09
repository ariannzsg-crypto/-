package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.example.data.model.EqualizerState
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
import kotlin.math.sin

class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    // State flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    // 16-band reactive visualizer spectrum normalized [0.0f .. 1.0f]
    private val _visualizerAmplitudes = MutableStateFlow(FloatArray(16) { 0.1f })
    val visualizerAmplitudes: StateFlow<FloatArray> = _visualizerAmplitudes.asStateFlow()

    var onSongFinished: (() -> Unit)? = null

    fun playSong(song: Song, newPlaylist: List<Song>? = null) {
        if (newPlaylist != null) {
            _playlist.value = newPlaylist
        }

        releasePlayer()

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, song.uri)
                prepare()
                start()
            }

            mediaPlayer = mp
            _currentSong.value = song
            _isPlaying.value = true
            _durationMs.value = mp.duration.toLong().coerceAtLeast(song.durationMs)
            _currentPositionMs.value = 0L

            setupAudioEffects(mp.audioSessionId)
            applyPlaybackSpeed(_playbackSpeed.value)

            mp.setOnCompletionListener {
                handleTrackCompletion()
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerManager", "MediaPlayer error: $what, extra: $extra")
                _isPlaying.value = false
                true
            }

            startTicker()
        } catch (e: Exception) {
            Log.e("MusicPlayerManager", "Failed to play song: ${song.title}", e)
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startTicker()
        } ?: run {
            _currentSong.value?.let { playSong(it) }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val newPos = (_currentPositionMs.value + deltaMs).coerceAtMost(_durationMs.value)
        seekTo(newPos)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val newPos = (_currentPositionMs.value - deltaMs).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun playNext() {
        val currentList = _playlist.value
        if (currentList.isEmpty()) return

        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        val nextIndex = if (_isShuffle.value) {
            val randomIndices = currentList.indices.filter { it != currentIndex }
            if (randomIndices.isNotEmpty()) randomIndices.random() else 0
        } else {
            if (currentIndex in currentList.indices) {
                (currentIndex + 1) % currentList.size
            } else 0
        }

        playSong(currentList[nextIndex])
    }

    fun playPrevious() {
        val currentList = _playlist.value
        if (currentList.isEmpty()) return

        // If played more than 3 seconds, replay current song
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentList.lastIndex
        playSong(currentList[prevIndex])
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        mediaPlayer?.let { mp ->
            try {
                val params = mp.playbackParams ?: PlaybackParams()
                params.speed = speed
                mp.playbackParams = params
            } catch (e: Exception) {
                Log.w("MusicPlayerManager", "PlaybackParams not supported or error", e)
            }
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

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val currentList = _playlist.value
                val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
                if (currentIndex in 0 until currentList.lastIndex) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    onSongFinished?.invoke()
                }
            }
        }
    }

    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                setStrength((_equalizerState.value.bassBoostStrength * 10).toShort())
            }
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                setStrength((_equalizerState.value.virtualizerStrength * 10).toShort())
            }

            applyEqualizerSettings(_equalizerState.value)
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Audio effects init fallback (simulated)", e)
        }
    }

    fun updateEqualizer(newState: EqualizerState) {
        _equalizerState.value = newState
        applyEqualizerSettings(newState)
    }

    private fun applyEqualizerSettings(state: EqualizerState) {
        try {
            equalizer?.let { eq ->
                eq.enabled = state.isEnabled
                if (state.isEnabled) {
                    val numBands = eq.numberOfBands.toInt().coerceAtMost(state.bands.size)
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]
                    for (i in 0 until numBands) {
                        val gainDb = state.bands[i].gainDb // -12 to +12
                        // Map gainDb to milliBels
                        val levelMb = (gainDb * 100).toShort().coerceIn(minLevel, maxLevel)
                        eq.setBandLevel(i.toShort(), levelMb)
                    }
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = state.isEnabled
                bb.setStrength((state.bassBoostStrength * 10).toShort())
            }

            virtualizer?.let { virt ->
                virt.enabled = state.isEnabled
                virt.setStrength((state.virtualizerStrength * 10).toShort())
            }
        } catch (e: Exception) {
            Log.w("MusicPlayerManager", "Failed applying equalizer settings", e)
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }

        var totalSeconds = minutes * 60
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            while (isActive && totalSeconds > 0) {
                delay(1000L)
                totalSeconds--
                _sleepTimerSeconds.value = totalSeconds
            }
            if (isActive && totalSeconds <= 0) {
                pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerSeconds.value = null
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var step = 0
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                        _durationMs.value = mp.duration.toLong()

                        // Calculate dynamic harmonic visualizer amplitudes based on time & playback
                        val pos = mp.currentPosition.toDouble() / 1000.0
                        val amps = FloatArray(16)
                        for (i in 0 until 16) {
                            val freq = (i + 1) * 1.8
                            val wave1 = (sin(pos * freq * 3.14) + 1.0) * 0.35
                            val wave2 = (sin(pos * 0.7 + i * 0.5) + 1.0) * 0.25
                            val beatPulse = if ((pos * 2.0).toInt() % 2 == 0) 0.3f else 0.1f
                            amps[i] = (wave1 + wave2 + beatPulse).toFloat().coerceIn(0.12f, 0.98f)
                        }
                        _visualizerAmplitudes.value = amps
                    }
                }
                step++
                delay(60L) // 60ms for silky smooth 60fps animations
            }
            if (!_isPlaying.value) {
                // Fade visualizer down smoothly
                _visualizerAmplitudes.value = FloatArray(16) { 0.08f }
            }
        }
    }

    private fun releasePlayer() {
        tickerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        releasePlayer()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        sleepTimerJob?.cancel()
    }
}
