package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val trackCount: Int,
    val albumCount: Int = 1,
    val artistArtUriString: String? = null
)
