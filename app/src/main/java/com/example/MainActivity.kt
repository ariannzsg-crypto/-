package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.ArtistsScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.TracksScreen
import com.example.ui.theme.MidnightBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MusicPlayerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(viewModel: MusicViewModel = viewModel()) {
    val context = LocalContext.current

    // State observations
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val recentlyPlayedIds by viewModel.recentlyPlayedIds.collectAsStateWithLifecycle()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isAutoPlay by viewModel.isAutoPlay.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsStateWithLifecycle()
    val visualizerAmplitudes by viewModel.visualizerAmplitudes.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isNowPlayingSheetOpen by viewModel.isNowPlayingSheetOpen.collectAsStateWithLifecycle()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val songForAddToPlaylist by viewModel.songForAddToPlaylist.collectAsStateWithLifecycle()
    var showVersionDialog by remember { mutableStateOf(false) }

    // File Picker for importing custom audio files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "Imported Track"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            viewModel.importAudioFile(uri, fileName)
            Toast.makeText(context, "فایل با موفقیت افزوده شد: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Folder Picker for scanning custom directories
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.scanFolder(uri)
            Toast.makeText(context, "در حال جستجو و اسکن آهنگ‌ها از پوشه...", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceAudio()
            Toast.makeText(context, "دسترسی تایید شد، اسکن آهنگ‌ها انجام شد", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "دسترسی برای اسکن حافظه داده نشد", Toast.LENGTH_SHORT).show()
        }
    }

    val requestMediaPermission = {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.scanDeviceAudio()
            Toast.makeText(context, "اسکن حافظه دستگاه انجام شد", Toast.LENGTH_SHORT).show()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Handle back button on sub-tabs
    BackHandler(enabled = currentTab != NavigationTab.TRACKS) {
        viewModel.selectTab(NavigationTab.TRACKS)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MidnightBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonViolet, NeonCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Ava Player",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "موزیک پلیر پیشرفته",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = NeonCyan
                            )
                        }
                    }
                },
                actions = {
                    // Sleep timer shortcut with badge
                    IconButton(onClick = { viewModel.setShowSleepTimerDialog(true) }) {
                        if (sleepTimerSeconds != null && sleepTimerSeconds!! > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = NeonPink) {
                                    Text("${sleepTimerSeconds!! / 60}m", fontSize = 8.sp)
                                }
                            }) {
                                Icon(Icons.Default.Bedtime, contentDescription = "Sleep Timer", tint = NeonPink)
                            }
                        } else {
                            Icon(Icons.Default.Bedtime, contentDescription = "Sleep Timer", tint = TextSecondary)
                        }
                    }

                    // Equalizer shortcut
                    IconButton(onClick = { viewModel.selectTab(NavigationTab.EQUALIZER) }) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = if (currentTab == NavigationTab.EQUALIZER) NeonCyan else TextSecondary
                        )
                    }

                    // Version Info button
                    IconButton(onClick = { showVersionDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Version Info",
                            tint = NeonViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MidnightBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Mini Player docked right above bottom navigation
                if (currentSong != null) {
                    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        progress = progress,
                        isFavorite = favoriteIds.contains(currentSong!!.id),
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playNext() },
                        onToggleFavorite = { viewModel.toggleFavorite(currentSong!!.id) },
                        onClick = { viewModel.setNowPlayingSheetOpen(true) }
                    )
                }

                // Modern M3 Navigation Bar with Neon accents
                NavigationBar(
                    containerColor = SurfaceCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.TRACKS,
                        onClick = { viewModel.selectTab(NavigationTab.TRACKS) },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Tracks") },
                        label = { Text("آهنگ‌ها", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = NeonViolet,
                            indicatorColor = NeonViolet.copy(alpha = 0.2f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("tab_tracks")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.PLAYLISTS,
                        onClick = { viewModel.selectTab(NavigationTab.PLAYLISTS) },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Playlists") },
                        label = { Text("لیست‌پخش", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("tab_playlists")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.ARTISTS,
                        onClick = { viewModel.selectTab(NavigationTab.ARTISTS) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Artists") },
                        label = { Text("هنرمندان", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            indicatorColor = NeonPink.copy(alpha = 0.2f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("tab_artists")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.ALBUMS,
                        onClick = { viewModel.selectTab(NavigationTab.ALBUMS) },
                        icon = { Icon(Icons.Default.Album, contentDescription = "Albums") },
                        label = { Text("آلبوم‌ها", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonViolet,
                            selectedTextColor = NeonViolet,
                            indicatorColor = NeonViolet.copy(alpha = 0.2f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("tab_albums")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.EQUALIZER,
                        onClick = { viewModel.selectTab(NavigationTab.EQUALIZER) },
                        icon = { Icon(Icons.Default.Equalizer, contentDescription = "Equalizer") },
                        label = { Text("اکولایزر", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("tab_equalizer")
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
                NavigationTab.TRACKS -> {
                    TracksScreen(
                        songs = songs,
                        folders = folders,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isAutoPlay = isAutoPlay,
                        favoriteIds = favoriteIds,
                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { viewModel.setSongForAddToPlaylist(it) },
                        onImportAudio = { filePickerLauncher.launch(arrayOf("audio/*")) },
                        onScanFolder = { folderPickerLauncher.launch(null) },
                        onScanDevice = requestMediaPermission,
                        onToggleAutoPlay = { viewModel.toggleAutoPlay() },
                        onPlayAllAuto = { list -> viewModel.playAllAuto(list) }
                    )
                }

                NavigationTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = playlists,
                        allSongs = songs,
                        favoriteIds = favoriteIds,
                        recentlyPlayedIds = recentlyPlayedIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onPlaySong = { song, list -> viewModel.playSong(song, list) },
                        onPlayAllPlaylist = { list -> if (list.isNotEmpty()) viewModel.playSong(list.first(), list) },
                        onCreatePlaylist = { viewModel.setShowCreatePlaylistDialog(true) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                NavigationTab.ARTISTS -> {
                    ArtistsScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        onPlaySong = { song, list -> viewModel.playSong(song, list) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                NavigationTab.ALBUMS -> {
                    AlbumsScreen(
                        songs = songs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        onPlaySong = { song, list -> viewModel.playSong(song, list) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                NavigationTab.EQUALIZER -> {
                    EqualizerScreen(
                        state = equalizerState,
                        onUpdateEqualizer = { viewModel.updateEqualizer(it) }
                    )
                }
            }
        }
    }

    // Full-Screen Now Playing Sheet
    if (isNowPlayingSheetOpen && currentSong != null) {
        NowPlayingSheet(
            song = currentSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            isFavorite = favoriteIds.contains(currentSong!!.id),
            visualizerAmplitudes = visualizerAmplitudes,
            playlist = currentPlaylist,
            sleepTimerSeconds = sleepTimerSeconds,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { viewModel.seekTo(it) },
            onSeekForward = { viewModel.seekForward() },
            onSeekBackward = { viewModel.seekBackward() },
            onNext = { viewModel.playNext() },
            onPrevious = { viewModel.playPrevious() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onToggleRepeat = { viewModel.toggleRepeat() },
            onToggleFavorite = { viewModel.toggleFavorite(currentSong!!.id) },
            onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
            onOpenEqualizer = {
                viewModel.setNowPlayingSheetOpen(false)
                viewModel.selectTab(NavigationTab.EQUALIZER)
            },
            onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
            onAddToPlaylist = { viewModel.setSongForAddToPlaylist(currentSong) },
            onSaveLyrics = { lyrics -> viewModel.saveLyrics(currentSong!!.id, lyrics) },
            onSelectQueueSong = { song -> viewModel.playSong(song, currentPlaylist) },
            onDismiss = { viewModel.setNowPlayingSheetOpen(false) }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingSeconds = sleepTimerSeconds,
            onSetTimer = { viewModel.setSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { viewModel.setShowSleepTimerDialog(false) }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.setShowCreatePlaylistDialog(false) },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                viewModel.setShowCreatePlaylistDialog(false)
                Toast.makeText(context, "لیست پخش «$name» ایجاد شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add To Playlist Dialog
    if (songForAddToPlaylist != null) {
        AddToPlaylistDialog(
            song = songForAddToPlaylist!!,
            playlists = playlists,
            onAddToPlaylist = { plId ->
                viewModel.addSongToPlaylist(plId, songForAddToPlaylist!!.id)
                viewModel.setSongForAddToPlaylist(null)
                Toast.makeText(context, "به لیست پخش اضافه شد", Toast.LENGTH_SHORT).show()
            },
            onCreateNewPlaylist = {
                viewModel.setShowCreatePlaylistDialog(true)
            },
            onDismiss = { viewModel.setSongForAddToPlaylist(null) }
        )
    }

    // Version & App Info Dialog
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشخصات و نسخه برنامه", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Ava Player (آوا پلیر)", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("نسخه (Version): 1.0.0", color = TextPrimary, fontSize = 14.sp)
                    Text("کد نسخه (Version Code): 1", color = TextSecondary, fontSize = 13.sp)
                    Text("شناسه بسته: com.aistudio.musicplayer.kzqpva", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("فایل‌های نسخه در پروژه:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("• VERSION (متن نسخه 1.0.0)", color = TextPrimary, fontSize = 12.sp)
                    Text("• version.json (اطلاعات فنی کامل JSON)", color = TextPrimary, fontSize = 12.sp)
                    Text("• نسخه_برنامه.txt (توضیحات و گزارش تغییرات)", color = TextPrimary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVersionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Text("بستن")
                }
            },
            containerColor = SurfaceCard
        )
    }
}
