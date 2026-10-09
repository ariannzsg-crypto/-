# 🎵 Ava Player (آوا پلیر)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room%20DB-2.7.0%20(KSP)-34A853.svg?style=for-the-badge&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-green.svg?style=for-the-badge&logo=android)](https://www.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

> **یک موزیک پلیر مدرن، قدرتمند و زیبا با تم ابسیدین و نئون، پایگاه داده محلی Room، ویژوالایزر زنده، اکولایزر ۵ کاناله و گرامافون چرخان.**
>
> *An ultra-modern, high-fidelity Android Music Player crafted with Jetpack Compose, Room Database (KSP), live audio visualizers, 5-band graphic equalizer, and animated vinyl turntable.*

---

## ✨ Features (امکانات و قابلیت‌ها)

### 🗄️ Room Database Local Persistence (پایگاه داده محلی Room)
- **Entities:** `SongEntity`, `AlbumEntity`, `ArtistEntity`, `PlaylistEntity`
- **Reactive Architecture:** Pure `Flow<List<T>>` queries via `MusicDao` for seamless UI updates
- **Local Library Sync:** Auto-indexing and caching of local audio files, albums, artists, and playlists
- **Favorites & History:** Persistent favorites tracking and play count metrics

### 🎶 Audio Engine & Playback Controls (موتور پخش پیشرفته)
- **High-Performance MediaPlayer:** Ultra-smooth scrubbing, 10s fast-forward / rewind
- **Dynamic Speed Control:** 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x playback speed rates
- **Looping & Shuffling:** Repeat Off, Repeat All, Repeat One, and Smart Shuffle
- **Built-in Studio Samples:** High-quality procedural stereo WAV audio tracks generated in-app for instant playback testing without requiring initial device files

### 🎛️ 5-Band Graphic Equalizer & Sound FX (اکولایزر ۵ کاناله و افکت‌ها)
- **Graphic Equalizer:** 60 Hz, 230 Hz, 910 Hz, 3.6 kHz, and 14 kHz sliders (-12 dB to +12 dB)
- **Presets:** Flat, Bass Boost, Rock, Pop, Jazz, Electronic, Vocal, Classical, Hip-Hop
- **Bass Boost:** Dedicated percentage slider for deep low-end punch
- **3D Surround (Virtualizer):** Immersive spatial audio processing

### 🪩 Reactive Visualizer & Animated Vinyl Turntable (ویژوالایزر و صفحه گرامافون)
- **4 Visualizer Modes:**
  1. *Spectrum Bars:* Neon glowing vertical equalizer bars
  2. *Neon Wave:* Sinuous, oscillating real-time waveform
  3. *Circular Pulse:* Beat-reactive radial energy rings
  4. *Ambient Halo:* Soft radial glow following musical rhythm
- **Animated Vinyl Turntable:** Rotating micro-groove disc, album cover spindle, and physical tone-arm needle moving into position during playback

### 📜 Synchronized Lyrics Viewer (متن هماهنگ ترانه)
- Auto-scrolling lyrics with real-time active verse highlighting
- Support for timestamped lyrics (`[mm:ss.xx]`) and plain text
- In-app custom lyrics editor

### 💎 Gorgeous Glassmorphic Mini Player (مینی‌پلیر شیشه‌ای شناور)
- Gradient glow border (`NeonViolet` to `NeonCyan` to `NeonPink`)
- Real-time animated miniature equalizer bars in the track header
- Spinning micro vinyl disc thumbnail
- Ultra-thin top gradient progress bar
- Instant touch controls (Play/Pause with pulse glow, Favorite heart, Next track)

### 📁 Library & Playlists (کتابخانه و لیست‌های پخش)
- Organized tabs: **Tracks**, **Playlists**, **Artists**, **Albums**, and **Equalizer**
- **Smart Playlists:** *Favorites* (علاقه‌مندی‌ها), *Recently Played* (اخیراً پخش شده), and *Most Played* (بیشترین پخش)
- Custom playlist creation and management
- Storage Access Framework (SAF) single-file audio importer + MediaStore scanner

---

## 🏗️ Architecture & Tech Stack

```
com.example/
├── data/
│   ├── audio/
│   │   ├── MusicPlayerManager.kt   # Core MediaPlayer controller & audio effects
│   │   └── WavAudioGenerator.kt    # In-memory synthesizer for sample tracks
│   ├── local/
│   │   ├── dao/
│   │   │   └── MusicDao.kt         # Reactive Room DAO with Flow
│   │   ├── entity/
│   │   │   ├── SongEntity.kt       # Song table
│   │   │   ├── AlbumEntity.kt      # Album table
│   │   │   ├── ArtistEntity.kt     # Artist table
│   │   │   └── PlaylistEntity.kt   # Playlist table
│   │   └── MusicDatabase.kt        # Room database builder
│   ├── model/
│   │   └── Song.kt                 # Domain models, enums & states
│   └── repository/
│       └── MusicRepository.kt      # Room & MediaStore unified repository
├── ui/
│   ├── components/
│   │   ├── AudioVisualizer.kt      # Canvas-based reactive visualizers
│   │   ├── LyricsView.kt           # Synced lyrics viewer & editor
│   │   ├── MiniPlayer.kt           # Enhanced floating glassmorphism player
│   │   ├── NowPlayingSheet.kt      # Fullscreen immersive player modal
│   │   ├── PlaylistDialogs.kt      # Dialogs for creating/adding playlists
│   │   ├── SleepTimerDialog.kt     # Preset & custom sleep timer
│   │   └── VinylTurntable.kt       # Animated vinyl record & tonearm
│   ├── screens/
│   │   ├── AlbumsScreen.kt
│   │   ├── ArtistsScreen.kt
│   │   ├── EqualizerScreen.kt
│   │   ├── PlaylistsScreen.kt
│   │   └── TracksScreen.kt
│   ├── theme/
│   │   ├── Color.kt                # Obsidian & Neon color system
│   │   ├── Theme.kt                # Material 3 dark theme
│   │   └── Type.kt                 # Typography
│   ├── MusicViewModel.kt           # State holder with StateFlow
│   └── MainActivity.kt             # Edge-to-Edge root Activity
```

- **Architecture:** Clean MVVM with Unidirectional Data Flow (UDF)
- **UI Toolkit:** 100% Jetpack Compose with Material Design 3
- **Local Storage:** Room Database 2.7.0 with Kotlin Symbol Processing (KSP)
- **Concurrency:** Kotlin Coroutines & Asynchronous Flow
- **Image Loading:** Coil Compose 2.7.0

---

## 🚀 Getting Started (نحوه اجرا)

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17 / JDK 21
- Android SDK 35/36
- Minimum SDK: Android 7.0 (API 24)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/your-username/ava-music-player.git
cd ava-music-player

# Build the Debug APK
gradle assembleDebug

# Run Unit and Robolectric Tests
gradle :app:testDebugUnitTest
```

---

## 🔒 Permissions & Privacy
Ava Player respects user privacy and complies with Google Play policies:
- `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`: Used exclusively to scan local songs on your device.
- `MODIFY_AUDIO_SETTINGS`: Required for the hardware Equalizer, Bass Boost, and Virtualizer effects.
- Zero analytics tracking, zero third-party telemetry, 100% local on-device operation.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
