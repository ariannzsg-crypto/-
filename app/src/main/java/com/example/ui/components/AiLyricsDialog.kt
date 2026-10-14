package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiLyricsResult
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

enum class AiLyricsTab {
    FULL_LYRICS,
    SYNC_LRC,
    TRANSLATION,
    ANALYSIS
}

@Composable
fun AiLyricsDialog(
    song: Song,
    isLoading: Boolean,
    result: AiLyricsResult?,
    onDismiss: () -> Unit,
    onSaveAndSync: (String) -> Unit,
    onReTranscribe: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AiLyricsTab.FULL_LYRICS) }
    var customPrompt by remember { mutableStateOf("") }
    var showCustomPromptInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(NeonViolet, NeonCyan)),
                    RoundedCornerShape(24.dp)
                )
                .testTag("ai_lyrics_dialog"),
            color = MidnightBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with AI Icon and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonViolet, NeonPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "هوش مصنوعی تبدیل آهنگ به متن",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gemini AI Audio-to-Lyrics Transcriber",
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_ai_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Song Target Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Music",
                                tint = NeonPink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${song.artist} • ${song.durationMs / 1000}s",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (result != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3306B6D4))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = result.detectedLanguage,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content State: Loading vs Results
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonViolet,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "در حال پردازش صوت و تبدیل به متن توسط هوش مصنوعی...",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Transcribing melody, syllables & synchronized LRC tags...",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        AudioVisualizer(
                            isPlaying = true,
                            style = VisualizerStyle.SPECTRUM_BARS,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(50.dp)
                        )
                    }
                } else if (result != null) {
                    // Result Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiTabChip(
                            title = "متن ترانه",
                            subtitle = "Lyrics",
                            isSelected = selectedTab == AiLyricsTab.FULL_LYRICS,
                            onClick = { selectedTab = AiLyricsTab.FULL_LYRICS },
                            modifier = Modifier.weight(1f)
                        )
                        AiTabChip(
                            title = "زمان‌بندی LRC",
                            subtitle = "Sync",
                            isSelected = selectedTab == AiLyricsTab.SYNC_LRC,
                            onClick = { selectedTab = AiLyricsTab.SYNC_LRC },
                            modifier = Modifier.weight(1f)
                        )
                        AiTabChip(
                            title = "ترجمه",
                            subtitle = "Translate",
                            isSelected = selectedTab == AiLyricsTab.TRANSLATION,
                            onClick = { selectedTab = AiLyricsTab.TRANSLATION },
                            modifier = Modifier.weight(1f)
                        )
                        AiTabChip(
                            title = "تحلیل آهنگ",
                            subtitle = "Analysis",
                            isSelected = selectedTab == AiLyricsTab.ANALYSIS,
                            onClick = { selectedTab = AiLyricsTab.ANALYSIS },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text Content Display Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceElevated)
                            .padding(14.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            val displayText = when (selectedTab) {
                                AiLyricsTab.FULL_LYRICS -> result.fullLyrics
                                AiLyricsTab.SYNC_LRC -> result.lrcContent
                                AiLyricsTab.TRANSLATION -> result.translation ?: "ترجمه برای این ترانه موجود نیست."
                                AiLyricsTab.ANALYSIS -> result.analysis ?: "تحلیل سبک صوتی موجود نیست."
                            }

                            Text(
                                text = displayText,
                                color = TextPrimary,
                                fontSize = if (selectedTab == AiLyricsTab.SYNC_LRC) 12.sp else 14.sp,
                                fontFamily = if (selectedTab == AiLyricsTab.SYNC_LRC) FontFamily.Monospace else FontFamily.Default,
                                lineHeight = if (selectedTab == AiLyricsTab.SYNC_LRC) 20.sp else 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row: Save & Sync, Copy, Share, Re-transcribe
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Apply & Sync Button
                        Button(
                            onClick = {
                                onSaveAndSync(result.lrcContent)
                                Toast.makeText(
                                    context,
                                    "متن همگام‌سازی شده با موفقیت به آهنگ اضافه شد!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("apply_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "همگام‌سازی در پلیر",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Copy Text
                        OutlinedButton(
                            onClick = {
                                val textToCopy = when (selectedTab) {
                                    AiLyricsTab.FULL_LYRICS -> result.fullLyrics
                                    AiLyricsTab.SYNC_LRC -> result.lrcContent
                                    AiLyricsTab.TRANSLATION -> result.translation ?: result.fullLyrics
                                    AiLyricsTab.ANALYSIS -> result.analysis ?: result.fullLyrics
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Lyrics", textToCopy)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "متن در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Share
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "${song.title} - متن ترانه")
                                    putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}\n\n${result.fullLyrics}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری متن ترانه"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = NeonPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Re-transcribe with prompt toggle
                        OutlinedButton(
                            onClick = { showCustomPromptInput = !showCustomPromptInput },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Optional Custom Prompt Input
                    AnimatedVisibility(visible = showCustomPromptInput) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = { customPrompt = it },
                                    placeholder = { Text("مثلاً: فقط کورس رو بنویس یا ترجمه روان‌تر...", fontSize = 11.sp, color = TextMuted) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        onReTranscribe(customPrompt.ifBlank { null })
                                        showCustomPromptInput = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("ارسال", fontSize = 11.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                } else {
                    // Empty / Initial prompt state
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = NeonViolet,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "تبدیل هوشمند آهنگ به متن ترانه",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "هوش مصنوعی جمینای صوت آهنگ را تحلیل کرده و متن دقیق همراه با تایم‌لاین همگام‌سازی می‌سازد.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onReTranscribe(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("شروع تبدیل آهنگ به متن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiTabChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonViolet.copy(alpha = 0.85f) else SurfaceCard)
            .border(
                1.dp,
                if (isSelected) NeonCyan else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) Color.White else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = if (isSelected) NeonCyan else TextMuted,
                fontSize = 9.sp
            )
        }
    }
}
