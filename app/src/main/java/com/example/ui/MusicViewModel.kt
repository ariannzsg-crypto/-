package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.MusicPlayerManager
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.model.EqualizerState
import com.example.data.model.Playlist
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
    TRACKS,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
    EQUALIZER
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val playerManager = MusicPlayerManager(application)

    // Reactive StateFlows from Room database via repository
    val songs: StateFlow<List<Song>> = repository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumEntity>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<ArtistEntity>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = songs
        .map { list -> list.filter { it.isFavorite }.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recentlyPlayedIds: StateFlow<List<String>> = repository.recentlyPlayedIds

    // Playback flows from player manager
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val currentPlaylist: StateFlow<List<Song>> = playerManager.playlist
    val equalizerState: StateFlow<EqualizerState> = playerManager.equalizerState
    val sleepTimerSeconds: StateFlow<Int?> = playerManager.sleepTimerSeconds
    val visualizerAmplitudes: StateFlow<FloatArray> = playerManager.visualizerAmplitudes

    // UI Navigation & Dialog States
    private val _currentTab = MutableStateFlow(NavigationTab.TRACKS)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isNowPlayingSheetOpen = MutableStateFlow(false)
    val isNowPlayingSheetOpen: StateFlow<Boolean> = _isNowPlayingSheetOpen.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _songForAddToPlaylist = MutableStateFlow<Song?>(null)
    val songForAddToPlaylist: StateFlow<Song?> = _songForAddToPlaylist.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun setNowPlayingSheetOpen(open: Boolean) {
        _isNowPlayingSheetOpen.value = open
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _showSleepTimerDialog.value = show
    }

    fun setShowCreatePlaylistDialog(show: Boolean) {
        _showCreatePlaylistDialog.value = show
    }

    fun setSongForAddToPlaylist(song: Song?) {
        _songForAddToPlaylist.value = song
    }

    // Playback control
    fun playSong(song: Song, playlist: List<Song>) {
        playerManager.playSong(song, playlist)
        viewModelScope.launch {
            repository.recordPlay(song.id)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun seekForward() {
        playerManager.seekForward(10000L)
    }

    fun seekBackward() {
        playerManager.seekBackward(10000L)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun updateEqualizer(state: EqualizerState) {
        playerManager.updateEqualizer(state)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    // Room Repository operations
    fun toggleFavorite(songId: String) {
        val isFav = favoriteIds.value.contains(songId)
        viewModelScope.launch {
            repository.toggleFavorite(songId, isFav)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun importAudioFile(uri: Uri, displayName: String) {
        viewModelScope.launch {
            repository.addImportedSong(uri, displayName)
        }
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            repository.refreshDeviceAudio()
        }
    }

    fun saveLyrics(songId: String, lyrics: String) {
        viewModelScope.launch {
            repository.updateLyrics(songId, lyrics)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
