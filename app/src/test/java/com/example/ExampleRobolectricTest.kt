package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MusicDatabase
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.SongEntity
import com.example.data.lyrics.LrcParser
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

    @Test
    fun `test LrcParser parsing and multiple time tags`() {
        val lrcSample = """
            [ti:Ava Song]
            [ar:Ava Artist]
            [al:Ava Album]
            [offset:500]
            [00:04.50]First line of the song
            [00:10.00][00:20.00]Repeated chorus line
            [01:05.123]Final verse
        """.trimIndent()

        val parsed = LrcParser.parse(lrcSample)

        assertEquals("Ava Song", parsed.title)
        assertEquals("Ava Artist", parsed.artist)
        assertEquals("Ava Album", parsed.album)
        assertEquals(500L, parsed.offsetMs)

        // Line 1: 4500 + 500 = 5000ms
        // Line 2 & 3: 10000 + 500 = 10500ms, and 20000 + 500 = 20500ms
        // Line 4: 65123 + 500 = 65623ms
        assertEquals(4, parsed.lines.size)
        assertEquals(5000L, parsed.lines[0].timeMs)
        assertEquals("First line of the song", parsed.lines[0].text)

        assertEquals(10500L, parsed.lines[1].timeMs)
        assertEquals("Repeated chorus line", parsed.lines[1].text)

        assertEquals(20500L, parsed.lines[2].timeMs)
        assertEquals("Repeated chorus line", parsed.lines[2].text)

        assertEquals(65623L, parsed.lines[3].timeMs)
        assertEquals("Final verse", parsed.lines[3].text)
    }

    @Test
    fun `test LrcParser active line index calculation and formatting`() {
        val lines = listOf(
            com.example.data.lyrics.LrcLine(5000L, "Intro"),
            com.example.data.lyrics.LrcLine(15000L, "Verse 1"),
            com.example.data.lyrics.LrcLine(30000L, "Chorus")
        )

        // Before first line
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 2000L))

        // On first line
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 8000L))

        // On second line
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 16000L))

        // On chorus
        assertEquals(2, LrcParser.findActiveLineIndex(lines, 35000L))

        // Test formatting
        assertEquals("[00:05.00]", LrcParser.formatTimeTag(5000L))
        assertEquals("[01:15.50]", LrcParser.formatTimeTag(75500L))
    }
}
