package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainTab
import com.example.ui.MusicViewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AiLyricsDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.ArtistsScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.TracksScreen
import com.example.ui.theme.AvaMusicPlayerTheme
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AvaMusicPlayerTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MusicViewModel) {
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val recentlyPlayedIds by viewModel.recentlyPlayedIds.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val isNowPlayingOpen by viewModel.isNowPlayingOpen.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerRemainingSeconds.collectAsState()

    // Dialogs state
    val showCreatePlaylist by viewModel.showCreatePlaylistDialog.collectAsState()
    val songForAddToPlaylist by viewModel.songForAddToPlaylist.collectAsState()
    val showSleepTimer by viewModel.showSleepTimerDialog.collectAsState()

    // AI Transcribe state
    val aiTranscribingSong by viewModel.aiTranscribingSong.collectAsState()
    val isAiTranscribing by viewModel.isAiTranscribing.collectAsState()
    val aiLyricsResult by viewModel.aiLyricsResult.collectAsState()

    // EQ state
    val equalizerPresets by viewModel.equalizerPresets.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()
    val bandFrequencies by viewModel.bandFrequencies.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val bassBoost by viewModel.bassBoostStrength.collectAsState()
    val virtualizer by viewModel.virtualizerStrength.collectAsState()

    // Permission Launcher: Requests media audio & storage permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            Toast.makeText(context, "دسترسی تأیید شد. در حال اسکن آهنگ‌ها...", Toast.LENGTH_SHORT).show()
            viewModel.scanStorage()
        }
    }

    // SAF Folder Picker Launcher: Lets user pick any directory on phone / SD card
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            Toast.makeText(context, "در حال اسکن پوشه انتخاب شده...", Toast.LENGTH_SHORT).show()
            viewModel.importFromFolder(treeUri)
        }
    }

    // Auto-request permission on launch if not already granted
    LaunchedEffect(Unit) {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermission = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            try {
                permissionLauncher.launch(permissionsToRequest)
            } catch (e: Throwable) {
                // Ignore launcher exceptions
            }
        }
    }

    // Handle back button when NowPlaying is expanded
    BackHandler(enabled = isNowPlayingOpen) {
        viewModel.closeNowPlaying()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MidnightBackground,
        topBar = {
            TopHeaderBar(
                isScanning = isScanning,
                sleepTimerSec = sleepTimerSec,
                onScanClick = { viewModel.scanStorage() },
                onSleepTimerClick = { viewModel.openSleepTimerDialog() }
            )
        },
        bottomBar = {
            Column {
                // Floating MiniPlayer above bottom navigation
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.playNext() },
                    onPreviousClick = { viewModel.playPrevious() },
                    onFavoriteClick = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                    onClick = { viewModel.openNowPlaying() }
                )

                // Navigation Bar
                NavigationBar(
                    containerColor = Color(0xF00D111D),
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.TRACKS,
                        onClick = { viewModel.setTab(MainTab.TRACKS) },
                        icon = { Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Tracks") },
                        label = { Text("آهنگ‌ها", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = NeonViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x33A855F7)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.PLAYLISTS,
                        onClick = { viewModel.setTab(MainTab.PLAYLISTS) },
                        icon = { Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "Playlists") },
                        label = { Text("پلی‌لیست", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x33EC4899)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.ARTISTS,
                        onClick = { viewModel.setTab(MainTab.ARTISTS) },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Artists") },
                        label = { Text("خواننده‌ها", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x3306B6D4)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.ALBUMS,
                        onClick = { viewModel.setTab(MainTab.ALBUMS) },
                        icon = { Icon(imageVector = Icons.Default.Album, contentDescription = "Albums") },
                        label = { Text("آلبوم‌ها", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = NeonViolet,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x33A855F7)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.EQUALIZER,
                        onClick = { viewModel.setTab(MainTab.EQUALIZER) },
                        icon = { Icon(imageVector = Icons.Default.Equalizer, contentDescription = "Equalizer") },
                        label = { Text("اکولایزر", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color(0x3306B6D4)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.TRACKS -> TracksScreen(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isScanning = isScanning,
                    onSongClick = { viewModel.playSong(it) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToPlaylistClick = { viewModel.openAddToPlaylistDialog(it) },
                    onScanStorageClick = { viewModel.scanStorage() },
                    onPickFolderClick = { folderPickerLauncher.launch(null) },
                    onAiTranscribeClick = { viewModel.openAiTranscribeDialog(it) }
                )

                MainTab.PLAYLISTS -> PlaylistsScreen(
                    playlists = playlists,
                    songs = songs,
                    favoriteSongIds = favoriteIds,
                    recentlyPlayedIds = recentlyPlayedIds,
                    onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                    onDeletePlaylistClick = { viewModel.deletePlaylist(it) },
                    onPlaylistClick = { pl ->
                        val plSongs = songs.filter { pl.songIds.contains(it.id) }
                        viewModel.playSongsList(plSongs)
                    },
                    onPlaySongsList = { viewModel.playSongsList(it) }
                )

                MainTab.ARTISTS -> ArtistsScreen(
                    songs = songs,
                    onPlaySong = { viewModel.playSong(it) }
                )

                MainTab.ALBUMS -> AlbumsScreen(
                    songs = songs,
                    onPlaySong = { viewModel.playSong(it) }
                )

                MainTab.EQUALIZER -> EqualizerScreen(
                    enabled = true,
                    presets = equalizerPresets,
                    currentPreset = currentPreset,
                    bandFrequencies = bandFrequencies,
                    bandLevels = bandLevels,
                    bassBoost = bassBoost,
                    virtualizer = virtualizer,
                    onPresetSelected = { viewModel.setEqualizerPreset(it) },
                    onBandLevelChange = { band, level -> viewModel.setBandLevel(band, level) },
                    onBassBoostChange = { viewModel.setBassBoost(it) },
                    onVirtualizerChange = { viewModel.setVirtualizer(it) }
                )
            }
        }
    }

    // Full-Screen NowPlaying Modal Sheet
    AnimatedVisibility(
        visible = isNowPlayingOpen,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NowPlayingSheet(
            song = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            queue = queue,
            sleepTimerRemainingSeconds = sleepTimerSec,
            onCloseClick = { viewModel.closeNowPlaying() },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onNextClick = { viewModel.playNext() },
            onPreviousClick = { viewModel.playPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onFavoriteToggle = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
            onRepeatToggle = { viewModel.toggleRepeat() },
            onShuffleToggle = { viewModel.toggleShuffle() },
            onAddToPlaylistClick = { currentSong?.let { viewModel.openAddToPlaylistDialog(it) } },
            onOpenSleepTimer = { viewModel.openSleepTimerDialog() },
            onSaveLyrics = { lrc -> currentSong?.let { viewModel.saveLyrics(it.id, lrc) } },
            onSelectQueueItem = { viewModel.playSong(it) },
            onAiTranscribeClick = { viewModel.openAiTranscribeDialog(it) }
        )
    }

    // Dialogs
    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.closeCreatePlaylistDialog() },
            onCreate = { viewModel.createPlaylist(it) }
        )
    }

    if (songForAddToPlaylist != null) {
        AddToPlaylistDialog(
            song = songForAddToPlaylist!!,
            playlists = playlists,
            onDismiss = { viewModel.closeAddToPlaylistDialog() },
            onPlaylistSelected = { plId ->
                viewModel.addSongToPlaylist(plId, songForAddToPlaylist!!.id)
            },
            onCreateNewPlaylist = {
                viewModel.openCreatePlaylistDialog()
            }
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            remainingSeconds = sleepTimerSec,
            onSetTimerMinutes = { viewModel.setSleepTimer(it) },
            onDismiss = { viewModel.closeSleepTimerDialog() }
        )
    }

    if (aiTranscribingSong != null) {
        AiLyricsDialog(
            song = aiTranscribingSong!!,
            isLoading = isAiTranscribing,
            result = aiLyricsResult,
            onDismiss = { viewModel.closeAiTranscribeDialog() },
            onSaveAndSync = { lrc ->
                viewModel.saveAiLyricsToSong(aiTranscribingSong!!.id, lrc)
            },
            onReTranscribe = { prompt ->
                viewModel.startAiTranscribe(aiTranscribingSong!!, prompt)
            }
        )
    }
}

@Composable
private fun TopHeaderBar(
    isScanning: Boolean,
    sleepTimerSec: Int?,
    onScanClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Glowing Logo
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NeonViolet, NeonPink)))
                .border(1.5.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ava Player",
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3306B6D4))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "موزیک پلیر پیشرفته و هوشمند",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // Sleep Timer Shortcut
        IconButton(
            onClick = onSleepTimerClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (sleepTimerSec != null) Color(0x3306B6D4) else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = "Sleep Timer",
                tint = if (sleepTimerSec != null) NeonCyan else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Scan Refresh Button
        IconButton(onClick = onScanClick) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Scan Storage",
                tint = if (isScanning) NeonPink else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
