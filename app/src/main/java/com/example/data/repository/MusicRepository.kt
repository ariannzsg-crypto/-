package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.audio.WavAudioGenerator
import com.example.data.local.MusicDatabaseHelper
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.scanner.AudioScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val dbHelper = MusicDatabaseHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    val favoritesFlow: StateFlow<Set<String>> = dbHelper.favoritesFlow
    val recentlyPlayedFlow: StateFlow<List<String>> = dbHelper.recentlyPlayedFlow
    val playlistsFlow: StateFlow<List<Playlist>> = dbHelper.playlistsFlow

    // Combined songs with favorite flag accurately updated
    val songsFlow: StateFlow<List<Song>> = combine(
        dbHelper.songsFlow,
        dbHelper.favoritesFlow
    ) { songs, favorites ->
        songs.map { song ->
            song.copy(isFavorite = favorites.contains(song.id))
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            try {
                // First ensure demo songs exist so user has immediate playable audio
                val demoSongs = WavAudioGenerator.getOrGenerateDemoSongs(context)
                if (demoSongs.isNotEmpty()) {
                    dbHelper.upsertSongs(demoSongs)
                }

                // Auto-scan device MediaStore & folders if permission granted
                scanDevice()
            } catch (e: Throwable) {
                // Non-fatal fallback
            }
        }
    }

    suspend fun scanDevice(): Int = withContext(Dispatchers.IO) {
        try {
            val mediaStoreSongs = AudioScanner.scanMediaStore(context)
            val folderSongs = AudioScanner.scanDeviceFolders(context)

            // Merge songs uniquely by URI / filePath
            val mergedMap = mutableMapOf<String, Song>()
            for (song in mediaStoreSongs) {
                mergedMap[song.filePath ?: song.id] = song
            }
            for (song in folderSongs) {
                val key = song.filePath ?: song.id
                if (!mergedMap.containsKey(key)) {
                    mergedMap[key] = song
                }
            }

            val allScanned = mergedMap.values.toList()
            if (allScanned.isNotEmpty()) {
                dbHelper.upsertSongs(allScanned)
            }
            allScanned.size
        } catch (e: Throwable) {
            0
        }
    }

    suspend fun importFromFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val songs = AudioScanner.scanDocumentTree(context, treeUri)
        if (songs.isNotEmpty()) {
            dbHelper.upsertSongs(songs)
        }
        songs.size
    }

    fun toggleFavorite(songId: String): Boolean {
        return dbHelper.toggleFavorite(songId)
    }

    fun incrementPlayCount(songId: String) {
        dbHelper.incrementPlayCount(songId)
    }

    fun updateLyrics(songId: String, lrcText: String) {
        dbHelper.updateLyrics(songId, lrcText)
    }

    fun createPlaylist(name: String): Long {
        return dbHelper.createPlaylist(name)
    }

    fun deletePlaylist(playlistId: Long) {
        dbHelper.deletePlaylist(playlistId)
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        dbHelper.addSongToPlaylist(playlistId, songId)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        dbHelper.removeSongFromPlaylist(playlistId, songId)
    }
}
