# Ava AI Music Player 🎵🤖

An Android music player built with **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and **Google Gemini AI**. 

Ava AI Music Player combines high-fidelity audio playback, full background service execution with lockscreen and system notification controls, real-time synchronized LRC lyrics, and AI-powered audio-to-lyrics transcription.

---

## ✨ Key Features

### 🎧 High-Performance Playback with AndroidX Media3 (ExoPlayer)
- **Background Media Playback**: Powered by `MusicPlaybackService` (`MediaSessionService`) ensuring uninterrupted background audio playback.
- **MediaStyle Notification Controls**: Native Android lockscreen and status bar controls with real-time playback state, seekbar, next/previous actions, and album artwork.
- **Audio Becoming Noisy**: Automatically pauses when headphones or Bluetooth devices disconnect.
- **Wake Lock Protection**: Ensures smooth, gapless playback when the screen is turned off.

### 🤖 Gemini AI Song-to-Lyrics Transcriber
- **Audio-to-Text Transcription**: Automatically transcribes audio tracks into lyrics using Gemini AI.
- **Synchronized LRC Generation**: Produces timestamped `[mm:ss.xx]` lines matching the song melody.
- **Bilingual Translation & Analysis**: Translates lyrics between Persian and English, and provides insights on musical genre, mood, and poetic theme.
- **One-Tap Player Sync**: Instantly applies transcribed lyrics to the local database and synchronizes scrolling playback in the player.

### 🎶 Real-Time Synchronized Lyrics
- Automatically highlights the active lyric line with smooth auto-scrolling as the track progresses.
- Interactive tap-to-seek: Tap any lyric line to jump directly to that timestamp.
- Full support for `.lrc` companion files and manual lyric editing.

### 🎛️ 5-Band Graphic Equalizer & Audio FX
- Graphic Equalizer with frequency bands (60 Hz, 230 Hz, 910 Hz, 3.6 kHz, 14 kHz).
- Built-in sound presets: Flat, Bass, Rock, Pop, Jazz, Electronic, Classical.
- Hardware-accelerated **Bass Boost** and **Virtualizer (3D Audio)** effects.

### 🎨 Visualizers & Modern UI
- Rotating **Vinyl Turntable** with animated tonearm and vinyl sheen.
- Multi-mode **Audio Visualizers**: Neon Spectrum Bars, Neon Waveform, and Radial Pulse.
- Dark theme with Neon Violet, Cyan, and Pink accents.
- Responsive Jetpack Compose layout with edge-to-edge support.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3)
- **Audio Engine**: AndroidX Media3 ExoPlayer `1.5.1` & `MediaSessionService`
- **AI Integration**: Google Gemini API via REST (`gemini-3.5-flash`)
- **Networking**: OkHttp `4.12.0`
- **Local Database**: SQLite persistent helper with reactive Kotlin `StateFlow`
- **Image Loading**: Coil Compose `2.7.0`
- **Design Pattern**: MVVM with Clean Architecture principles

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or JDK 21
- Android SDK 35/36 (Minimum SDK 24 - Android 7.0+)

### API Key Configuration
To enable the Gemini AI audio transcription feature, configure your Gemini API Key in your environment or via the Secrets panel:
```bash
# In .env file
GEMINI_API_KEY=your_gemini_api_key_here
```

### Build & Run
```bash
# Assemble Debug APK
gradle assembleDebug

# Run unit tests
gradle testDebugUnitTest
```

---

## 📄 License
This project is open-source and available under the Apache 2.0 License.
