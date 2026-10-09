package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.example.R
import com.example.data.audio.WavAudioGenerator
import com.example.data.local.MusicDatabase
import com.example.data.local.dao.MusicDao
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao = MusicDatabase.getDatabase(context).musicDao()
) {

    // Reactive Flow streams directly from Room
    val songs: Flow<List<Song>> = musicDao.getAllSongs().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val playlists: Flow<List<Playlist>> = musicDao.getAllPlaylists().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val albums: Flow<List<AlbumEntity>> = musicDao.getAllAlbums()

    val artists: Flow<List<ArtistEntity>> = musicDao.getAllArtists()

    private val _recentlyPlayedIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyPlayedIds: StateFlow<List<String>> = _recentlyPlayedIds.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val existingCount = musicDao.getSongCount()
        if (existingCount == 0) {
            // Seed built-in songs into Room
            val builtInSongs = generateBuiltInSongs()
            val entities = builtInSongs.map { SongEntity.fromDomainModel(it) }
            musicDao.insertSongs(entities)

            // Seed default playlists
            val p1 = PlaylistEntity(
                id = "pl_synth",
                name = "Cyber & Synth / سینث‌ویو",
                songIdsRaw = builtInSongs.filter { it.album.contains("Cyber") || it.album.contains("Pulse") }.joinToString(",") { it.id }
            )
            val p2 = PlaylistEntity(
                id = "pl_chill",
                name = "Chill & Relax / آرامش‌بخش",
                songIdsRaw = builtInSongs.filter { it.album.contains("Desert") || it.album.contains("Rainy") }.joinToString(",") { it.id }
            )
            musicDao.insertPlaylist(p1)
            musicDao.insertPlaylist(p2)
        }

        // Scan device audio and sync into Room
        syncDeviceAudioWithRoom()
    }

    private suspend fun syncDeviceAudioWithRoom() = withContext(Dispatchers.IO) {
        val deviceSongs = scanDeviceAudio()
        if (deviceSongs.isNotEmpty()) {
            val entities = deviceSongs.map { SongEntity.fromDomainModel(it) }
            musicDao.insertSongs(entities)
        }
        deriveAndStoreAlbumsAndArtists()
    }

    private suspend fun deriveAndStoreAlbumsAndArtists() = withContext(Dispatchers.IO) {
        // Fetch current songs from MediaStore/Built-in to populate Albums and Artists tables
        val allTracks = generateBuiltInSongs() + scanDeviceAudio()

        // Derive Albums
        val albumGroups = allTracks.groupBy { it.album }
        val albumEntities = albumGroups.map { (albumTitle, tracks) ->
            val first = tracks.first()
            AlbumEntity(
                id = "album_${albumTitle.hashCode()}",
                title = albumTitle,
                artist = first.artist,
                songCount = tracks.size,
                albumArtRes = first.albumArtRes,
                albumArtUriString = first.albumArtUri?.toString()
            )
        }
        musicDao.insertAlbums(albumEntities)

        // Derive Artists
        val artistGroups = allTracks.groupBy { it.artist }
        val artistEntities = artistGroups.map { (artistName, tracks) ->
            val albumCount = tracks.map { it.album }.distinct().size
            ArtistEntity(
                id = "artist_${artistName.hashCode()}",
                name = artistName,
                trackCount = tracks.size,
                albumCount = albumCount,
                artistArtUriString = tracks.firstOrNull()?.albumArtUri?.toString()
            )
        }
        musicDao.insertArtists(artistEntities)
    }

    private fun generateBuiltInSongs(): List<Song> {
        val list = mutableListOf<Song>()
        try {
            // Track 1: Neon Horizon
            val f1 = WavAudioGenerator.generateSampleTrack(context, "neon_horizon.wav", 1)
            list.add(
                Song(
                    id = "builtin_1",
                    title = "Neon Horizon (افق نئونی)",
                    artist = "Aria Nova",
                    album = "Cyber Dreams 2088",
                    durationMs = 18000L,
                    uri = Uri.fromFile(f1),
                    albumArtRes = R.drawable.album_art_neon,
                    lyrics = """
[00:00.00] ♫ Neon Horizon - Instrumental Intro ♫
[00:02.00] Passing through the glowing streets of neon light
[00:05.00] Synthesizers pulsing through the midnight sky
[00:08.00] Speeding faster on the cyber highway
[00:11.00] Bassline driving all the echoes away
[00:14.00] Electric heart, infinite beat!
[00:16.50] ♫ Melodic Synth Climax ♫
                    """.trimIndent(),
                    isBuiltIn = true
                )
            )

            // Track 2: Ava Shoushtari
            val f2 = WavAudioGenerator.generateSampleTrack(context, "ava_shoushtari.wav", 2)
            list.add(
                Song(
                    id = "builtin_2",
                    title = "نوای شوشتری (Ava Shoushtari)",
                    artist = "استاد همایون آوا",
                    album = "گلبانگ کویر (Desert Melodies)",
                    durationMs = 18000L,
                    uri = Uri.fromFile(f2),
                    albumArtRes = R.drawable.album_art_chill,
                    lyrics = """
[00:00.00] ♫ پیش‌درآمد تار و تنبک در گوشه شوشتری ♫
[00:03.00] ای ساربان آهسته ران کآرام جانم می‌رود
[00:07.00] وآن دل که با خود داشتم با دلستانم می‌رود
[00:11.00] محمل بدار ای ساروان تندی مکن با کاروان
[00:14.00] کز عشق آن سرو روان گویی روانم می‌رود
[00:17.00] ♫ تکنوازی احساسی و پرده‌گردانی ♫
                    """.trimIndent(),
                    isBuiltIn = true
                )
            )

            // Track 3: Midnight Lo-Fi Beats
            val f3 = WavAudioGenerator.generateSampleTrack(context, "midnight_lofi.wav", 3)
            list.add(
                Song(
                    id = "builtin_3",
                    title = "Midnight Coffee (قهوه نیمه‌شب)",
                    artist = "Lofi Soul",
                    album = "Rainy Bedroom Sessions",
                    durationMs = 18000L,
                    uri = Uri.fromFile(f3),
                    albumArtRes = R.drawable.album_art_chill,
                    lyrics = """
[00:00.00] ♫ Warm Vinyl Crackle & Rhodes Chords ♫
[00:03.00] Gentle raindrops tapping on the window glass
[00:06.00] A warm mug in hand, watching night hours pass
[00:10.00] Calm thoughts flowing like a steady stream
[00:13.00] Lost in a cozy peaceful twilight dream
[00:16.00] ♫ Soothing Bassline & Mellow Keys ♫
                    """.trimIndent(),
                    isBuiltIn = true
                )
            )

            // Track 4: Cyber Pulse
            val f4 = WavAudioGenerator.generateSampleTrack(context, "cyber_pulse.wav", 4)
            list.add(
                Song(
                    id = "builtin_4",
                    title = "Cyber Pulse (ضربان سایبری)",
                    artist = "Hyperion",
                    album = "Future Velocity",
                    durationMs = 18000L,
                    uri = Uri.fromFile(f4),
                    albumArtRes = R.drawable.album_art_neon,
                    lyrics = """
[00:00.00] ♫ High Energy Electro Arp ♫
[00:03.00] Feel the frequency rising in your veins
[00:06.00] Breaking through the sound barriers and chains
[00:10.00] Maximum power, bass overdrive
[00:13.00] When the music plays, we are alive!
[00:16.00] ♫ Peak Drop & Sub-bass Resonance ♫
                    """.trimIndent(),
                    isBuiltIn = true
                )
            )
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed generating sample tracks", e)
        }
        return list
    }

    suspend fun scanDeviceAudio(): List<Song> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val albumId = c.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = "content://media/external/audio/albumart/$albumId".toUri()

                    if (duration > 5000) {
                        result.add(
                            Song(
                                id = "device_$id",
                                title = title,
                                artist = if (artist == "<unknown>") "Artist" else artist,
                                album = if (album == "<unknown>") "Album" else album,
                                durationMs = duration,
                                uri = contentUri,
                                albumArtUri = albumArtUri,
                                isBuiltIn = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Device scan permission or query warning: ${e.message}")
        }

        result
    }

    suspend fun refreshDeviceAudio() {
        syncDeviceAudioWithRoom()
    }

    suspend fun addImportedSong(uri: Uri, title: String, artist: String = "Imported", durationMs: Long = 60000L) = withContext(Dispatchers.IO) {
        val newSong = Song(
            id = "imported_${System.currentTimeMillis()}",
            title = title,
            artist = artist,
            album = "Imported Files",
            durationMs = durationMs,
            uri = uri,
            albumArtRes = R.drawable.album_art_neon,
            isBuiltIn = false
        )
        musicDao.insertSong(SongEntity.fromDomainModel(newSong))
        deriveAndStoreAlbumsAndArtists()
    }

    suspend fun toggleFavorite(songId: String, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        musicDao.updateFavorite(songId, !currentFavorite)
    }

    suspend fun recordPlay(songId: String) = withContext(Dispatchers.IO) {
        musicDao.incrementPlayCount(songId)
        val recent = _recentlyPlayedIds.value.toMutableList()
        recent.remove(songId)
        recent.add(0, songId)
        if (recent.size > 50) recent.removeAt(recent.lastIndex)
        _recentlyPlayedIds.value = recent
    }

    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()) = withContext(Dispatchers.IO) {
        val newPl = PlaylistEntity(
            id = "pl_${System.currentTimeMillis()}",
            name = name,
            songIdsRaw = songIds.joinToString(",")
        )
        musicDao.insertPlaylist(newPl)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val existing = musicDao.getPlaylistById(playlistId) ?: return@withContext
        val currentIds = if (existing.songIdsRaw.isBlank()) emptyList() else existing.songIdsRaw.split(",").filter { it.isNotBlank() }
        if (!currentIds.contains(songId)) {
            val updated = currentIds + songId
            musicDao.updatePlaylistSongIds(playlistId, updated.joinToString(","))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val existing = musicDao.getPlaylistById(playlistId) ?: return@withContext
        val currentIds = if (existing.songIdsRaw.isBlank()) emptyList() else existing.songIdsRaw.split(",").filter { it.isNotBlank() }
        val updated = currentIds.filter { it != songId }
        musicDao.updatePlaylistSongIds(playlistId, updated.joinToString(","))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun updateLyrics(songId: String, lyrics: String) = withContext(Dispatchers.IO) {
        musicDao.updateLyrics(songId, lyrics)
    }
}
