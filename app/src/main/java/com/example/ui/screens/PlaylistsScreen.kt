package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
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
import com.example.ui.theme.WarmGold

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    favoriteIds: Set<String>,
    recentlyPlayedIds: List<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAllPlaylist: (List<Song>) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var smartPlaylistTitle by remember { mutableStateOf<String?>(null) }
    var smartPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }

    val activeList = selectedPlaylist?.let { pl ->
        allSongs.filter { pl.songIds.contains(it.id) }
    } ?: smartPlaylistSongs

    val activeTitle = selectedPlaylist?.name ?: smartPlaylistTitle

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (activeList != null && activeTitle != null) {
            // Detailed Playlist View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        selectedPlaylist = null
                        smartPlaylistTitle = null
                        smartPlaylistSongs = null
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTitle,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeList.size} آهنگ",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (activeList.isNotEmpty()) {
                    IconButton(
                        onClick = { onPlayAllPlaylist(activeList) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonViolet)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White)
                    }
                }
            }

            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("این لیست پخش خالی است", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeList) { song ->
                        SongItemRow(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
                            onClick = { onPlaySong(song, activeList) },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                            onAddToPlaylist = {}
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        } else {
            // Playlists Overview
            Spacer(modifier = Modifier.height(12.dp))

            // Smart Playlists Row
            Text(
                text = "پلی‌لیست‌های هوشمند (Smart Playlists)",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Favorites Card
                val favSongs = allSongs.filter { favoriteIds.contains(it.id) }
                SmartPlaylistCard(
                    title = "موردعلاقه‌ها",
                    count = "${favSongs.size} آهنگ",
                    icon = Icons.Default.Favorite,
                    gradient = Brush.linearGradient(listOf(NeonPink, Color(0xFF9D174D))),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        smartPlaylistTitle = "علاقه‌مندی‌ها (Favorites)"
                        smartPlaylistSongs = favSongs
                    }
                )

                // Recently Played Card
                val recentSongs = recentlyPlayedIds.mapNotNull { id -> allSongs.find { it.id == id } }
                SmartPlaylistCard(
                    title = "اخیراً پخش شده",
                    count = "${recentSongs.size} آهنگ",
                    icon = Icons.Default.History,
                    gradient = Brush.linearGradient(listOf(NeonCyan, Color(0xFF0F766E))),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        smartPlaylistTitle = "اخیراً پخش شده (Recently Played)"
                        smartPlaylistSongs = recentSongs
                    }
                )

                // Most Played Card
                val mostPlayedSongs = allSongs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
                SmartPlaylistCard(
                    title = "بیشترین پخش",
                    count = "${mostPlayedSongs.size} آهنگ",
                    icon = Icons.Default.TrendingUp,
                    gradient = Brush.linearGradient(listOf(WarmGold, Color(0xFFB45309))),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        smartPlaylistTitle = "بیشترین پخش (Most Played)"
                        smartPlaylistSongs = mostPlayedSongs
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Playlists Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست‌های پخش شما (${playlists.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onCreatePlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_playlist_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ساخت لیست جدید", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playlists List
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنوز لیست پخشی ایجاد نکرده‌اید",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists) { pl ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedPlaylist = pl },
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = NeonViolet,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${pl.songIds.size} آهنگ",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDeletePlaylist(pl.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Playlist",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
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
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = count,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
