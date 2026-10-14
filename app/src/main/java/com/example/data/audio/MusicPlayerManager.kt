package com.example.data.audio

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class MusicPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val player: ExoPlayer
    private var mediaSession: MediaSession? = null

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

    private val _bassBoostStrength = MutableStateFlow(500)
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(300)
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _bandFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies.asStateFlow()

    private val _bandLevels = MutableStateFlow<Map<Short, Short>>(emptyMap())
    val bandLevels: StateFlow<Map<Short, Short>> = _bandLevels.asStateFlow()

    var onSongFinished: ((Song) -> Unit)? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        setupMediaSession()
        setupPlayerListeners()
    }

    private fun setupMediaSession() {
        try {
            val sessionActivityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                sessionActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            mediaSession = MediaSession.Builder(context, player)
                .setSessionActivity(pendingIntent)
                .build()
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "MediaSession init skipped", e)
        }
    }

    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                    startService()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val dur = player.duration
                        if (dur > 0) {
                            _durationMs.value = dur
                        }
                        initAudioEffects(player.audioSessionId)
                    }
                    Player.STATE_ENDED -> {
                        handleSongCompletion()
                    }
                    else -> {}
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
            }
        })
    }

    private fun startService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Could not start MusicPlaybackService directly", e)
        }
    }

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
                // Prepare item in player without starting
                loadSongIntoPlayer(song, playImmediately = false)
            }
        }
    }

    fun playSong(song: Song, startPlaying: Boolean = true) {
        _currentSong.value = song
        _durationMs.value = song.durationMs
        _currentPositionMs.value = 0L

        loadSongIntoPlayer(song, playImmediately = startPlaying)
    }

    private fun loadSongIntoPlayer(song: Song, playImmediately: Boolean) {
        try {
            val mediaUri = if (song.filePath != null && File(song.filePath).exists()) {
                Uri.fromFile(File(song.filePath))
            } else {
                song.uri
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.albumArtUri)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(mediaUri)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()

            if (playImmediately) {
                player.play()
                _isPlaying.value = true
                startProgressTracker()
                startService()
            } else {
                _isPlaying.value = false
            }
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Failed to load song in ExoPlayer: ${song.title}", e)
            _isPlaying.value = false
            stopProgressTracker()
        }
    }

    private fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Throwable) { }

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
                val centerFreq = try {
                    equalizer?.getCenterFreq(band)?.div(1000) ?: ((i + 1) * 200)
                } catch (e: Throwable) {
                    (i + 1) * 200
                }
                freqs.add(centerFreq)
                levels[band] = try { equalizer?.getBandLevel(band) ?: 0 } catch (e: Throwable) { 0 }
            }
            _bandFrequencies.value = freqs
            _bandLevels.value = levels
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Equalizer unavailable on this device", e)
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
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } else {
            val cur = _currentSong.value
            if (cur != null) {
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    playSong(cur, true)
                } else {
                    player.play()
                    _isPlaying.value = true
                    startProgressTracker()
                    startService()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
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
            player.pause()
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
                player.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                val curIndex = q.indexOfFirst { it.id == cur?.id }
                if (curIndex in 0 until q.size - 1) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setSleepTimer(minutes: Int?) {
        if (minutes == null || minutes <= 0) {
            cancelSleepTimer()
        } else {
            startSleepTimer(minutes)
        }
    }

    fun startSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        _sleepTimerRemainingSeconds.value = minutes * 60

        sleepTimerJob?.cancel()
        sleepTimerJob = scope.launch {
            var remaining = minutes * 60
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
                _sleepTimerMinutes.value = null
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = null
        _sleepTimerRemainingSeconds.value = null
    }

    fun setEqualizerPreset(presetName: String) {
        _currentPreset.value = presetName
        try {
            val presets = _equalizerPresets.value
            val idx = presets.indexOf(presetName)
            if (idx >= 0) {
                equalizer?.usePreset(idx.toShort())
                val levels = mutableMapOf<Short, Short>()
                val bands = equalizer?.numberOfBands ?: 0
                for (b in 0 until bands) {
                    val band = b.toShort()
                    levels[band] = equalizer?.getBandLevel(band) ?: 0
                }
                _bandLevels.value = levels
            }
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Failed to apply preset $presetName", e)
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            val updated = _bandLevels.value.toMutableMap()
            updated[band] = level
            _bandLevels.value = updated
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Failed to set band level", e)
        }
    }

    fun setBassBoost(strength: Int) {
        _bassBoostStrength.value = strength
        try {
            bassBoost?.setStrength(strength.toShort())
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Failed to set bass boost", e)
        }
    }

    fun setVirtualizer(strength: Int) {
        _virtualizerStrength.value = strength
        try {
            virtualizer?.setStrength(strength.toShort())
        } catch (e: Throwable) {
            Log.w("MusicPlayerManager", "Failed to set virtualizer", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && player.isPlaying) {
                _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(100L) // Smooth 100ms updates for precise lyrics sync
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun updateCurrentSongLyrics(lrcContent: String) {
        val cur = _currentSong.value ?: return
        val updated = cur.copy(lrcContent = lrcContent)
        _currentSong.value = updated
        _queue.value = _queue.value.map { if (it.id == cur.id) updated else it }
    }

    fun release() {
        stopProgressTracker()
        sleepTimerJob?.cancel()
        try {
            player.release()
            mediaSession?.release()
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Throwable) {
            Log.e("MusicPlayerManager", "Error in release", e)
        }
    }
}
