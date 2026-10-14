package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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
    onAiTranscribeClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val haptic = LocalHapticFeedback.current
    var currentTab by remember { mutableStateOf(NowPlayingTab.TURNTABLE) }
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.SPECTRUM_BARS) }

    // Apple Music style interactive horizontal swipe gesture
    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "apple_music_swipe"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .testTag("now_playing_sheet")
    ) {
        // Apple Music style Ambient Glowing Aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonViolet.copy(alpha = 0.22f),
                            NeonCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAiTranscribeClick(song) }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Transcribe",
                            tint = NeonViolet
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

            // Main Display Area based on selected Tab with Apple Music swipe to change song
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                    .then(
                        if (currentTab == NowPlayingTab.TURNTABLE || currentTab == NowPlayingTab.VISUALIZER) {
                            Modifier.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (swipeOffset < -130f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNextClick()
                                        } else if (swipeOffset > 130f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPreviousClick()
                                        }
                                        swipeOffset = 0f
                                    },
                                    onDragCancel = {
                                        swipeOffset = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeOffset = (swipeOffset + dragAmount).coerceIn(-300f, 300f)
                                    }
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Apple Music Swipe Direction Indicators
                if (swipeOffset < -60f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE606B6D4))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("آهنگ بعدی", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                } else if (swipeOffset > 60f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE6A855F7))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("آهنگ قبلی", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        if (swipeOffset <= 0) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut())
                        }
                    },
                    label = "apple_music_song_change"
                ) { currentSongState ->
                    when (currentTab) {
                        NowPlayingTab.TURNTABLE -> {
                            VinylTurntable(
                                song = currentSongState,
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
                                onAiTranscribeClick = { onAiTranscribeClick(song) },
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

            // Track Details & Actions Row with Apple Music smooth text transitions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 3 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    },
                    modifier = Modifier.weight(1f),
                    label = "apple_music_title_anim"
                ) { curSong ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = curSong.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = curSong.artist,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

            // Timestamps (Apple Music format: elapsed / -remaining)
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
                    text = formatRemainingMs(currentPositionMs, durationMs),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Apple Music Style Bottom Quick-Access Bar (Lyrics, Visualizer, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics Toggle (Apple Music Speech Bubble / Subtitles icon)
                IconButton(
                    onClick = {
                        currentTab = if (currentTab == NowPlayingTab.LYRICS) NowPlayingTab.TURNTABLE else NowPlayingTab.LYRICS
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (currentTab == NowPlayingTab.LYRICS) Color(0x33A855F7) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Lyrics",
                        tint = if (currentTab == NowPlayingTab.LYRICS) NeonViolet else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Visualizer Toggle
                IconButton(
                    onClick = {
                        currentTab = if (currentTab == NowPlayingTab.VISUALIZER) NowPlayingTab.TURNTABLE else NowPlayingTab.VISUALIZER
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (currentTab == NowPlayingTab.VISUALIZER) Color(0x3306B6D4) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Visualizer",
                        tint = if (currentTab == NowPlayingTab.VISUALIZER) NeonCyan else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Up Next Queue Toggle
                IconButton(
                    onClick = {
                        currentTab = if (currentTab == NowPlayingTab.QUEUE) NowPlayingTab.TURNTABLE else NowPlayingTab.QUEUE
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (currentTab == NowPlayingTab.QUEUE) Color(0x33EC4899) else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Up Next Queue",
                        tint = if (currentTab == NowPlayingTab.QUEUE) NeonPink else TextMuted,
                        modifier = Modifier.size(24.dp)
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
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun formatRemainingMs(currentMs: Long, durationMs: Long): String {
    if (durationMs <= 0) return "-00:00"
    val remaining = (durationMs - currentMs).coerceAtLeast(0L)
    val totalSec = remaining / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "-%02d:%02d".format(min, sec)
}
