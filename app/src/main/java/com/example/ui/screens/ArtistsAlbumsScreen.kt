package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArtistsScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    val artistsMap = remember(songs) {
        songs.groupBy { it.artist }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (selectedArtist != null) {
            val artistSongs = artistsMap[selectedArtist] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedArtist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedArtist ?: "",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artistSongs.size} قطعه موسیقی",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (artistSongs.isNotEmpty()) {
                    IconButton(
                        onClick = { onPlaySong(artistSongs.first(), artistSongs) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonViolet)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artistSongs) { song ->
                    SongItemRow(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        isFavorite = favoriteIds.contains(song.id),
                        onClick = { onPlaySong(song, artistSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onAddToPlaylist = {}
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "هنرمندان (${artistsMap.size})",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(artistsMap.keys.toList()) { artistName ->
                    val artistTracks = artistsMap[artistName] ?: emptyList()
                    val sampleSong = artistTracks.firstOrNull()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedArtist = artistName },
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceElevated)
                                    .border(2.dp, NeonViolet, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sampleSong?.albumArtRes != null) {
                                    Image(
                                        painter = painterResource(id = sampleSong.albumArtRes),
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NeonViolet,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = artistName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${artistTracks.size} آهنگ",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedAlbum by remember { mutableStateOf<String?>(null) }

    val albumsMap = remember(songs) {
        songs.groupBy { it.album }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (selectedAlbum != null) {
            val albumSongs = albumsMap[selectedAlbum] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedAlbum = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedAlbum ?: "",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${albumSongs.size} قطعه • ${albumSongs.firstOrNull()?.artist ?: ""}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (albumSongs.isNotEmpty()) {
                    IconButton(
                        onClick = { onPlaySong(albumSongs.first(), albumSongs) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albumSongs) { song ->
                    SongItemRow(
                        song = song,
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying,
                        isFavorite = favoriteIds.contains(song.id),
                        onClick = { onPlaySong(song, albumSongs) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onAddToPlaylist = {}
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "آلبوم‌ها (${albumsMap.size})",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albumsMap.keys.toList()) { albumName ->
                    val albumTracks = albumsMap[albumName] ?: emptyList()
                    val sampleSong = albumTracks.firstOrNull()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAlbum = albumName },
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(SurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sampleSong?.albumArtRes != null) {
                                    Image(
                                        painter = painterResource(id = sampleSong.albumArtRes),
                                        contentDescription = albumName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (sampleSong?.albumArtUri != null) {
                                    AsyncImage(
                                        model = sampleSong.albumArtUri,
                                        contentDescription = albumName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = albumName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sampleSong?.artist ?: "Various Artists",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${albumTracks.size} آهنگ",
                                    color = NeonViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
