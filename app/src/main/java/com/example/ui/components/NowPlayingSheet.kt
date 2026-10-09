package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.model.VisualizerStyle
import com.example.ui.theme.MidnightBackground
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

enum class PlayerCenterView {
    VINYL_TURNTABLE,
    COVER_AND_VISUALIZER,
    LYRICS,
    QUEUE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    isFavorite: Boolean,
    visualizerAmplitudes: FloatArray,
    playlist: List<Song>,
    sleepTimerSeconds: Int?,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSaveLyrics: (String) -> Unit,
    onSelectQueueSong: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    if (song == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var centerView by remember { mutableStateOf(PlayerCenterView.VINYL_TURNTABLE) }
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM_BARS) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Smooth dragging seekbar state
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderValue = if (isDraggingSlider) {
        sliderDragPosition
    } else {
        if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightBackground,
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp).testTag("close_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "در حال پخش / NOW PLAYING",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = song.album,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sleep timer badge
                    IconButton(onClick = onOpenSleepTimer) {
                        if (sleepTimerSeconds != null && sleepTimerSeconds > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = NeonPink) {
                                    Text("${sleepTimerSeconds / 60}m", fontSize = 9.sp)
                                }
                            }) {
                                Icon(Icons.Default.Bedtime, contentDescription = "Sleep Timer", tint = NeonPink)
                            }
                        } else {
                            Icon(Icons.Default.Bedtime, contentDescription = "Sleep Timer", tint = TextSecondary)
                        }
                    }

                    // Equalizer shortcut
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = NeonViolet)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Center View Switcher Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CenterViewPill(
                    icon = Icons.Default.GraphicEq,
                    label = "گرامافون",
                    isSelected = centerView == PlayerCenterView.VINYL_TURNTABLE,
                    onClick = { centerView = PlayerCenterView.VINYL_TURNTABLE }
                )
                CenterViewPill(
                    icon = Icons.Default.MusicNote,
                    label = "ویژوالایزر",
                    isSelected = centerView == PlayerCenterView.COVER_AND_VISUALIZER,
                    onClick = { centerView = PlayerCenterView.COVER_AND_VISUALIZER }
                )
                CenterViewPill(
                    icon = Icons.Default.Lyrics,
                    label = "متن ترانه",
                    isSelected = centerView == PlayerCenterView.LYRICS,
                    onClick = { centerView = PlayerCenterView.LYRICS }
                )
                CenterViewPill(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "صف پخش",
                    isSelected = centerView == PlayerCenterView.QUEUE,
                    onClick = { centerView = PlayerCenterView.QUEUE }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    PlayerCenterView.VINYL_TURNTABLE -> {
                        VinylTurntable(
                            song = song,
                            isPlaying = isPlaying,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    PlayerCenterView.COVER_AND_VISUALIZER -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Cover art card with visualizer underlay
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceCard)
                                    .border(2.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
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
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Reactive Visualizer Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                AudioVisualizer(
                                    amplitudes = visualizerAmplitudes,
                                    isPlaying = isPlaying,
                                    style = visualizerStyle
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Visualizer style quick selector
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VisualizerStyle.entries.forEach { style ->
                                    val isCur = visualizerStyle == style
                                    Text(
                                        text = when (style) {
                                            VisualizerStyle.SPECTRUM_BARS -> "ستون‌ها"
                                            VisualizerStyle.WAVE_NEON -> "موج نئون"
                                            VisualizerStyle.CIRCULAR_PULSE -> "پالس دایره‌ای"
                                            VisualizerStyle.VINYL_RECORD -> "هاله"
                                        },
                                        color = if (isCur) NeonCyan else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCur) SurfaceHighlight else Color.Transparent)
                                            .clickable { visualizerStyle = style }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    PlayerCenterView.LYRICS -> {
                        LyricsView(
                            rawLyrics = song.lyrics,
                            currentPositionMs = currentPositionMs,
                            onSeekTo = onSeekTo,
                            onSaveLyrics = onSaveLyrics,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    PlayerCenterView.QUEUE -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceCard.copy(alpha = 0.85f))
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "صف پخش (${playlist.size} آهنگ)",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            itemsIndexed(playlist) { index, itemSong ->
                                val isCurrent = itemSong.id == song.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) SurfaceHighlight else Color.Transparent)
                                        .clickable { onSelectQueueSong(itemSong) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isCurrent) NeonCyan else TextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = itemSong.title,
                                            color = if (isCurrent) NeonCyan else TextPrimary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = itemSong.artist,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                    if (isCurrent && isPlaying) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // Song Info & Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Add to Playlist
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Favorite Heart Toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) NeonPink else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragPosition = it
                    },
                    onValueChangeFinished = {
                        val targetMs = (sliderDragPosition * durationMs).toLong()
                        onSeekTo(targetMs)
                        isDraggingSlider = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceHighlight
                    )
                )

                // Time labels
                val displayPosMs = if (isDraggingSlider) (sliderDragPosition * durationMs).toLong() else currentPositionMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(displayPosMs),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDuration(durationMs),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle Mode
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp).testTag("shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) NeonCyan else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Rewind 10 seconds
                IconButton(
                    onClick = onSeekBackward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp).testTag("prev_track_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Giant Glowing Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape, spotColor = NeonViolet)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonViolet, NeonCyan)
                            )
                        )
                        .clickable { onTogglePlayPause() }
                        .testTag("full_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next Track
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp).testTag("next_track_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Fast Forward 10 seconds
                IconButton(
                    onClick = onSeekForward,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Repeat Mode (Off, All, One)
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(44.dp).testTag("repeat_btn")
                ) {
                    when (repeatMode) {
                        RepeatMode.OFF -> Icon(Icons.Default.Repeat, contentDescription = "Repeat Off", tint = TextMuted, modifier = Modifier.size(22.dp))
                        RepeatMode.ALL -> Icon(Icons.Default.Repeat, contentDescription = "Repeat All", tint = NeonCyan, modifier = Modifier.size(22.dp))
                        RepeatMode.ONE -> Icon(Icons.Default.RepeatOne, contentDescription = "Repeat One", tint = NeonPink, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Sub-Bar: Playback Speed Selector & Sound Effects
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Speed Selector Pill
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .clickable { showSpeedMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${playbackSpeed}x سرعت",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${speed}x",
                                        color = if (playbackSpeed == speed) NeonCyan else TextPrimary,
                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSetPlaybackSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                // Equalizer button pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .clickable { onOpenEqualizer() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اکولایزر (EQ)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CenterViewPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NeonViolet else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}
