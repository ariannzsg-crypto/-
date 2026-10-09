package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val songIdsRaw: String = "", // Comma-separated song IDs
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Playlist {
        val list = if (songIdsRaw.isBlank()) emptyList() else songIdsRaw.split(",").filter { it.isNotBlank() }
        return Playlist(
            id = id,
            name = name,
            songIds = list,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomainModel(playlist: Playlist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                songIdsRaw = playlist.songIds.joinToString(","),
                createdAt = playlist.createdAt
            )
        }
    }
}
