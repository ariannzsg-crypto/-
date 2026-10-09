package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song
import androidx.core.net.toUri

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtRes: Int? = null,
    val albumArtUriString: String? = null,
    val lyrics: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val isBuiltIn: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uriString.toUri(),
            albumArtRes = albumArtRes,
            albumArtUri = albumArtUriString?.toUri(),
            lyrics = lyrics,
            isFavorite = isFavorite,
            playCount = playCount,
            isBuiltIn = isBuiltIn
        )
    }

    companion object {
        fun fromDomainModel(song: Song): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                uriString = song.uri.toString(),
                albumArtRes = song.albumArtRes,
                albumArtUriString = song.albumArtUri?.toString(),
                lyrics = song.lyrics,
                isFavorite = song.isFavorite,
                playCount = song.playCount,
                isBuiltIn = song.isBuiltIn
            )
        }
    }
}
