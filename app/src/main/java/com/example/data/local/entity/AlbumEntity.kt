package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUriString: String? = null,
    val albumArtRes: Int? = null
)
