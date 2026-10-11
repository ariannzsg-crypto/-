package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.MusicPlayerManager
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    TRACKS,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
    EQUALIZER
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val playerManager = MusicPlayerManager(application)

    val songs: StateFlow<List<Song>> = repository.songsFlow
    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow
    val favoriteSongIds: StateFlow<Set<String>> = repository.favoritesFlow
    val recentlyPlayedIds: StateFlow<List<String>> = repository.recentlyPlayedFlow

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val queue: StateFlow<List<Song>> = playerManager.queue
    val sleepTimerRemainingSeconds: StateFlow<Int?> = playerManager.sleepTimerRemainingSeconds

    // Audio effects
    val equalizerEnabled: StateFlow<Boolean> = playerManager.equalizerEnabled
    val equalizerPresets: StateFlow<List<String>> = playerManager.equalizerPresets
    val currentPreset: StateFlow<String> = playerManager.currentPreset
    val bandFrequencies: StateFlow<List<Int>> = playerManager.bandFrequencies
    val bandLevels: StateFlow<Map<Short, Short>> = playerManager.bandLevels
    val bassBoostStrength: StateFlow<Int> = playerManager.bassBoostStrength
    val virtualizerStrength: StateFlow<Int> = playerManager.virtualizerStrength

    // UI Navigation State
    private val _currentTab = MutableStateFlow(MainTab.TRACKS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isNowPlayingOpen = MutableStateFlow(false)
    val isNowPlayingOpen: StateFlow<Boolean> = _isNowPlayingOpen.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Dialogs
    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _songForAddToPlaylist = MutableStateFlow<Song?>(null)
    val songForAddToPlaylist: StateFlow<Song?> = _songForAddToPlaylist.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    init {
        playerManager.onSongFinished = { song ->
            repository.incrementPlayCount(song.id)
        }

        // Auto-initialize queue once songs are loaded
        viewModelScope.launch {
            songs.collect { songList ->
                if (songList.isNotEmpty() && currentSong.value == null) {
                    playerManager.setQueue(songList, startIndex = 0, autoPlay = false)
                }
            }
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun openNowPlaying() {
        _isNowPlayingOpen.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingOpen.value = false
    }

    fun playSong(song: Song) {
        val currentQueue = songs.value
        val index = currentQueue.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            playerManager.setQueue(currentQueue, index, autoPlay = true)
        } else {
            playerManager.playSong(song, startPlaying = true)
        }
        repository.incrementPlayCount(song.id)
    }

    fun playSongsList(list: List<Song>, startIndex: Int = 0) {
        if (list.isNotEmpty()) {
            playerManager.setQueue(list, startIndex, autoPlay = true)
            repository.incrementPlayCount(list[startIndex].id)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleFavorite(songId: String) {
        repository.toggleFavorite(songId)
    }

    fun toggleRepeat() {
        playerManager.toggleRepeatMode()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun setSleepTimer(minutes: Int?) {
        playerManager.setSleepTimer(minutes)
    }

    fun openSleepTimerDialog() {
        _showSleepTimerDialog.value = true
    }

    fun closeSleepTimerDialog() {
        _showSleepTimerDialog.value = false
    }

    fun saveLyrics(songId: String, lrcText: String) {
        repository.updateLyrics(songId, lrcText)
        // Also update in-memory current song if matched
        val cur = currentSong.value
        if (cur?.id == songId) {
            // refresh
        }
    }

    fun scanStorage() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanDevice()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importFromFolder(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.importFromFolder(uri)
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Playlist Dialog handlers
    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String) {
        repository.createPlaylist(name)
        _showCreatePlaylistDialog.value = false
    }

    fun deletePlaylist(id: Long) {
        repository.deletePlaylist(id)
    }

    fun openAddToPlaylistDialog(song: Song) {
        _songForAddToPlaylist.value = song
    }

    fun closeAddToPlaylistDialog() {
        _songForAddToPlaylist.value = null
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        repository.addSongToPlaylist(playlistId, songId)
        _songForAddToPlaylist.value = null
    }

    // EQ controls
    fun setEqualizerPreset(preset: String) {
        playerManager.setEqualizerPreset(preset)
    }

    fun setBandLevel(band: Short, level: Short) {
        playerManager.setBandLevel(band, level)
    }

    fun setBassBoost(strength: Int) {
        playerManager.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        playerManager.setVirtualizer(strength)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
