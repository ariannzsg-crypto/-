package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerStyle
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class NowPlayingTab {
    TURNTABLE,
    VISUALIZER,
    LYRICS,
    QUEUE
}

@Composable
fun NowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    queue: List<Song>,
    sleepTimerRemainingSeconds: Int?,
    onCloseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFavoriteToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onSaveLyrics: (String) -> Unit,
    onSelectQueueItem: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    var currentTab by remember { mutableStateOf(NowPlayingTab.TURNTABLE) }
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM_BARS) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .testTag("now_playing_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "در حال پخش",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = song.album,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onOpenSleepTimer) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerRemainingSeconds != null) NeonCyan else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabPill(
                    title = "گرامافون",
                    selected = currentTab == NowPlayingTab.TURNTABLE,
                    onClick = { currentTab = NowPlayingTab.TURNTABLE }
                )
                TabPill(
                    title = "ویژوالایزر",
                    selected = currentTab == NowPlayingTab.VISUALIZER,
                    onClick = { currentTab = NowPlayingTab.VISUALIZER }
                )
                TabPill(
                    title = "متن ترانه",
                    selected = currentTab == NowPlayingTab.LYRICS,
                    onClick = { currentTab = NowPlayingTab.LYRICS }
                )
                TabPill(
                    title = "صف پخش",
                    selected = currentTab == NowPlayingTab.QUEUE,
                    onClick = { currentTab = NowPlayingTab.QUEUE }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Display Area based on selected Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = currentTab, label = "tab_anim") { tab ->
                    when (tab) {
                        NowPlayingTab.TURNTABLE -> {
                            VinylTurntable(
                                song = song,
                                isPlaying = isPlaying,
                                modifier = Modifier.size(280.dp)
                            )
                        }

                        NowPlayingTab.VISUALIZER -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Album art thumbnail with glow
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(2.dp, NeonViolet, RoundedCornerShape(20.dp))
                                ) {
                                    if (song.albumArtRes != null) {
                                        Image(
                                            painter = painterResource(id = song.albumArtRes),
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (song.albumArtUri != null) {
                                        AsyncImage(
                                            model = song.albumArtUri,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.album_art_neon),
                                            contentDescription = "Cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                AudioVisualizer(
                                    isPlaying = isPlaying,
                                    style = visualizerStyle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Visualizer Style Selector
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VisualizerStyle.values().forEach { st ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (visualizerStyle == st) NeonViolet else SurfaceCard)
                                                .clickable { visualizerStyle = st }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = when (st) {
                                                    VisualizerStyle.SPECTRUM_BARS -> "ستون‌های نئون"
                                                    VisualizerStyle.WAVE_NEON -> "موج صدا"
                                                    VisualizerStyle.RADIAL_PULSE -> "پالس شعاعی"
                                                },
                                                color = if (visualizerStyle == st) Color.White else TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        NowPlayingTab.LYRICS -> {
                            LyricsView(
                                lrcContent = song.lrcContent,
                                currentPositionMs = currentPositionMs,
                                onSeekTo = onSeekTo,
                                onSaveLyrics = onSaveLyrics,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        NowPlayingTab.QUEUE -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(queue) { idx, qSong ->
                                    val isCurrent = qSong.id == song.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCurrent) Color(0x33A855F7) else SurfaceCard)
                                            .clickable { onSelectQueueItem(qSong) }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = if (isCurrent) NeonCyan else TextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.width(28.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qSong.title,
                                                color = if (isCurrent) NeonCyan else TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = qSong.artist,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Track Details & Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) NeonPink else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = onAddToPlaylistClick) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Scrubber Slider
            val progress = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeekTo((frac * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonViolet,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("track_slider")
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = formatMs(durationMs),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onShuffleToggle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) NeonPink else TextMuted
                    )
                }

                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonViolet, NeonPink)))
                        .border(2.dp, NeonCyan, CircleShape)
                        .clickable { onPlayPauseClick() }
                        .testTag("sheet_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onRepeatToggle) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) NeonCyan else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) NeonViolet else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
