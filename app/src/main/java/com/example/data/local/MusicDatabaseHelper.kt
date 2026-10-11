package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ava_music.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_SONGS = "songs"
        private const val COL_SONG_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_ARTIST = "artist"
        private const val COL_ALBUM = "album"
        private const val COL_DURATION = "duration_ms"
        private const val COL_URI = "uri"
        private const val COL_ART_RES = "art_res"
        private const val COL_ART_URI = "art_uri"
        private const val COL_FILE_PATH = "file_path"
        private const val COL_LRC = "lrc_content"
        private const val COL_FAVORITE = "is_favorite"
        private const val COL_PLAY_COUNT = "play_count"
        private const val COL_LAST_PLAYED = "last_played"
        private const val COL_DATE_ADDED = "date_added"

        private const val TABLE_PLAYLISTS = "playlists"
        private const val COL_PL_ID = "id"
        private const val COL_PL_NAME = "name"
        private const val COL_PL_CREATED = "created_at"

        private const val TABLE_PL_SONGS = "playlist_songs"
        private const val COL_PLS_PL_ID = "playlist_id"
        private const val COL_PLS_SONG_ID = "song_id"
    }

    private val _songsFlow = MutableStateFlow<List<Song>>(emptyList())
    val songsFlow: StateFlow<List<Song>> = _songsFlow.asStateFlow()

    private val _playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val playlistsFlow: StateFlow<List<Playlist>> = _playlistsFlow.asStateFlow()

    private val _favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    private val _recentlyPlayedFlow = MutableStateFlow<List<String>>(emptyList())
    val recentlyPlayedFlow: StateFlow<List<String>> = _recentlyPlayedFlow.asStateFlow()

    init {
        refreshState()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SONGS (
                $COL_SONG_ID TEXT PRIMARY KEY,
                $COL_TITLE TEXT NOT NULL,
                $COL_ARTIST TEXT NOT NULL,
                $COL_ALBUM TEXT,
                $COL_DURATION INTEGER,
                $COL_URI TEXT NOT NULL,
                $COL_ART_RES INTEGER,
                $COL_ART_URI TEXT,
                $COL_FILE_PATH TEXT,
                $COL_LRC TEXT,
                $COL_FAVORITE INTEGER DEFAULT 0,
                $COL_PLAY_COUNT INTEGER DEFAULT 0,
                $COL_LAST_PLAYED INTEGER DEFAULT 0,
                $COL_DATE_ADDED INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                $COL_PL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PL_NAME TEXT NOT NULL,
                $COL_PL_CREATED INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PL_SONGS (
                $COL_PLS_PL_ID INTEGER,
                $COL_PLS_SONG_ID TEXT,
                PRIMARY KEY ($COL_PLS_PL_ID, $COL_PLS_SONG_ID)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PL_SONGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        onCreate(db)
    }

    fun upsertSongs(songs: List<Song>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (song in songs) {
                val cv = ContentValues().apply {
                    put(COL_SONG_ID, song.id)
                    put(COL_TITLE, song.title)
                    put(COL_ARTIST, song.artist)
                    put(COL_ALBUM, song.album)
                    put(COL_DURATION, song.durationMs)
                    put(COL_URI, song.uri.toString())
                    put(COL_ART_RES, song.albumArtRes)
                    put(COL_ART_URI, song.albumArtUri?.toString())
                    put(COL_FILE_PATH, song.filePath)
                    if (song.lrcContent != null) put(COL_LRC, song.lrcContent)
                    put(COL_DATE_ADDED, song.dateAdded)
                }
                // Insert or ignore / update metadata while preserving favorite & play count
                val cursor = db.rawQuery(
                    "SELECT $COL_FAVORITE, $COL_PLAY_COUNT, $COL_LRC FROM $TABLE_SONGS WHERE $COL_SONG_ID = ?",
                    arrayOf(song.id)
                )
                if (cursor.moveToFirst()) {
                    val fav = cursor.getInt(0)
                    val count = cursor.getInt(1)
                    val existingLrc = cursor.getString(2)
                    cv.put(COL_FAVORITE, fav)
                    cv.put(COL_PLAY_COUNT, count)
                    if (song.lrcContent == null && existingLrc != null) {
                        cv.put(COL_LRC, existingLrc)
                    }
                    db.update(TABLE_SONGS, cv, "$COL_SONG_ID = ?", arrayOf(song.id))
                } else {
                    cv.put(COL_FAVORITE, if (song.isFavorite) 1 else 0)
                    cv.put(COL_PLAY_COUNT, song.playCount)
                    db.insertWithOnConflict(TABLE_SONGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                cursor.close()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshState()
    }

    fun toggleFavorite(songId: String): Boolean {
        val db = writableDatabase
        var newFavState = false
        val cursor = db.rawQuery(
            "SELECT $COL_FAVORITE FROM $TABLE_SONGS WHERE $COL_SONG_ID = ?",
            arrayOf(songId)
        )
        if (cursor.moveToFirst()) {
            val current = cursor.getInt(0)
            newFavState = (current == 0)
            val cv = ContentValues().apply {
                put(COL_FAVORITE, if (newFavState) 1 else 0)
            }
            db.update(TABLE_SONGS, cv, "$COL_SONG_ID = ?", arrayOf(songId))
        }
        cursor.close()
        refreshState()
        return newFavState
    }

    fun incrementPlayCount(songId: String) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_SONGS SET $COL_PLAY_COUNT = $COL_PLAY_COUNT + 1, $COL_LAST_PLAYED = ? WHERE $COL_SONG_ID = ?",
            arrayOf(System.currentTimeMillis(), songId)
        )
        refreshState()
    }

    fun updateLyrics(songId: String, lrcText: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_LRC, lrcText)
        }
        db.update(TABLE_SONGS, cv, "$COL_SONG_ID = ?", arrayOf(songId))
        refreshState()
    }

    fun createPlaylist(name: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_PL_NAME, name)
            put(COL_PL_CREATED, System.currentTimeMillis())
        }
        val id = db.insert(TABLE_PLAYLISTS, null, cv)
        refreshState()
        return id
    }

    fun deletePlaylist(playlistId: Long) {
        val db = writableDatabase
        db.delete(TABLE_PL_SONGS, "$COL_PLS_PL_ID = ?", arrayOf(playlistId.toString()))
        db.delete(TABLE_PLAYLISTS, "$COL_PL_ID = ?", arrayOf(playlistId.toString()))
        refreshState()
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_PLS_PL_ID, playlistId)
            put(COL_PLS_SONG_ID, songId)
        }
        db.insertWithOnConflict(TABLE_PL_SONGS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        refreshState()
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        val db = writableDatabase
        db.delete(
            TABLE_PL_SONGS,
            "$COL_PLS_PL_ID = ? AND $COL_PLS_SONG_ID = ?",
            arrayOf(playlistId.toString(), songId)
        )
        refreshState()
    }

    fun refreshState() {
        val db = readableDatabase
        val songsList = mutableListOf<Song>()
        val favs = mutableSetOf<String>()
        val recent = mutableListOf<String>()

        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_SONGS ORDER BY $COL_TITLE ASC",
            null
        )
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_SONG_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
            val artist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST))
            val album = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALBUM)) ?: "Unknown"
            val duration = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION))
            val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_URI))
            val artRes = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_ART_RES))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COL_ART_RES))
            val artUriStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_ART_URI))
            val filePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_PATH))
            val lrc = cursor.getString(cursor.getColumnIndexOrThrow(COL_LRC))
            val isFav = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FAVORITE)) == 1
            val count = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PLAY_COUNT))
            val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_ADDED))

            if (isFav) favs.add(id)

            songsList.add(
                Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    uri = Uri.parse(uriStr),
                    albumArtRes = artRes,
                    albumArtUri = if (!artUriStr.isNullOrEmpty()) Uri.parse(artUriStr) else null,
                    filePath = filePath,
                    lrcContent = lrc,
                    isFavorite = isFav,
                    playCount = count,
                    dateAdded = dateAdded
                )
            )
        }
        cursor.close()

        val recentCursor = db.rawQuery(
            "SELECT $COL_SONG_ID FROM $TABLE_SONGS WHERE $COL_LAST_PLAYED > 0 ORDER BY $COL_LAST_PLAYED DESC LIMIT 30",
            null
        )
        while (recentCursor.moveToNext()) {
            recent.add(recentCursor.getString(0))
        }
        recentCursor.close()

        // Fetch playlists
        val plList = mutableListOf<Playlist>()
        val plCursor = db.rawQuery("SELECT * FROM $TABLE_PLAYLISTS ORDER BY $COL_PL_NAME ASC", null)
        while (plCursor.moveToNext()) {
            val plId = plCursor.getLong(plCursor.getColumnIndexOrThrow(COL_PL_ID))
            val plName = plCursor.getString(plCursor.getColumnIndexOrThrow(COL_PL_NAME))
            val plCreated = plCursor.getLong(plCursor.getColumnIndexOrThrow(COL_PL_CREATED))

            val songIds = mutableListOf<String>()
            val plsCursor = db.rawQuery(
                "SELECT $COL_PLS_SONG_ID FROM $TABLE_PL_SONGS WHERE $COL_PLS_PL_ID = ?",
                arrayOf(plId.toString())
            )
            while (plsCursor.moveToNext()) {
                songIds.add(plsCursor.getString(0))
            }
            plsCursor.close()

            plList.add(
                Playlist(
                    id = plId,
                    name = plName,
                    songIds = songIds,
                    createdAt = plCreated
                )
            )
        }
        plCursor.close()

        _songsFlow.value = songsList
        _favoritesFlow.value = favs
        _recentlyPlayedFlow.value = recent
        _playlistsFlow.value = plList
    }
}
