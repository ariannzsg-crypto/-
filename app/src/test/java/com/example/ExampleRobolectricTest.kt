package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MusicDatabase
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: MusicDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ava Player", appName)
    }

    @Test
    fun `insert and retrieve songs from Room database`() = runBlocking {
        val dao = db.musicDao()
        val song = SongEntity(
            id = "test_1",
            title = "Test Melodic Track",
            artist = "Aria Nova",
            album = "Cyber Dreams",
            durationMs = 18000L,
            uriString = "file:///sample.wav",
            isFavorite = false,
            playCount = 0
        )
        dao.insertSong(song)

        val allSongs = dao.getAllSongs().first()
        assertEquals(1, allSongs.size)
        assertEquals("Test Melodic Track", allSongs[0].title)

        // Test updating favorite
        dao.updateFavorite("test_1", true)
        val favSongs = dao.getFavoriteSongs().first()
        assertEquals(1, favSongs.size)
        assertTrue(favSongs[0].isFavorite)

        // Test incrementing play count
        dao.incrementPlayCount("test_1")
        val mostPlayed = dao.getMostPlayedSongs().first()
        assertEquals(1, mostPlayed.size)
        assertEquals(1, mostPlayed[0].playCount)
    }

    @Test
    fun `insert albums artists and playlists into Room`() = runBlocking {
        val dao = db.musicDao()
        dao.insertAlbums(
            listOf(
                AlbumEntity(
                    id = "alb_1",
                    title = "Cyber Dreams",
                    artist = "Aria Nova",
                    songCount = 1
                )
            )
        )
        dao.insertArtists(
            listOf(
                ArtistEntity(
                    id = "art_1",
                    name = "Aria Nova",
                    trackCount = 1
                )
            )
        )
        dao.insertPlaylist(
            PlaylistEntity(
                id = "pl_1",
                name = "Workout Beats",
                songIdsRaw = "test_1"
            )
        )

        val albums = dao.getAllAlbums().first()
        val artists = dao.getAllArtists().first()
        val playlists = dao.getAllPlaylists().first()

        assertEquals(1, albums.size)
        assertEquals(1, artists.size)
        assertEquals(1, playlists.size)
        assertEquals("Workout Beats", playlists[0].name)
    }

    @Test
    fun `test folder querying in Room database`() = runBlocking {
        val dao = db.musicDao()
        dao.insertSong(
            SongEntity(
                id = "folder_song_1",
                title = "Folder Track 1",
                artist = "Artist A",
                album = "Album A",
                durationMs = 20000L,
                uriString = "file:///sdcard/Music/track1.mp3",
                folderName = "Music"
            )
        )
        dao.insertSong(
            SongEntity(
                id = "folder_song_2",
                title = "Folder Track 2",
                artist = "Artist B",
                album = "Album B",
                durationMs = 30000L,
                uriString = "file:///sdcard/Download/track2.mp3",
                folderName = "Download"
            )
        )

        val folders = dao.getAllFolders().first()
        assertTrue(folders.contains("Music"))
        assertTrue(folders.contains("Download"))

        val musicSongs = dao.getSongsByFolder("Music").first()
        assertEquals(1, musicSongs.size)
        assertEquals("Folder Track 1", musicSongs[0].title)
    }
}
