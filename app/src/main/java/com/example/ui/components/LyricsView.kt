package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.lyrics.LrcParser
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LyricsView(
    lrcContent: String?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (String) -> Unit,
    onAiTranscribeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val parsedLyrics = remember(lrcContent) {
        LrcParser.parse(lrcContent)
    }

    val activeIndex = remember(parsedLyrics, currentPositionMs) {
        LrcParser.findActiveIndex(parsedLyrics, currentPositionMs)
    }

    val listState = rememberLazyListState()
    var showEditDialog by remember { mutableStateOf(false) }

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex in parsedLyrics.indices) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (parsedLyrics.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33A855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Transcribe",
                        tint = NeonViolet,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "متن ترانه برای این آهنگ ثبت نشده است",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "با هوش مصنوعی جمینای می‌توانید صوت آهنگ را آنالیز کرده و متن کامل و زمان‌بندی شده دریافت کنید.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: AI Transcribe
                Button(
                    onClick = onAiTranscribeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .testTag("ai_transcribe_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تبدیل به متن با هوش مصنوعی (AI)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Manual Edit
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ورود دستی متن یا فایل LRC",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 100.dp, bottom = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lyrics_list")
            ) {
                itemsIndexed(parsedLyrics) { index, lyric ->
                    val isActive = index == activeIndex
                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> NeonCyan
                            index < activeIndex -> TextSecondary.copy(alpha = 0.5f)
                            else -> TextSecondary
                        },
                        animationSpec = tween(300),
                        label = "lyric_color"
                    )

                    val fontSize = if (isActive) 20.sp else 15.sp
                    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(lyric.timestampMs) }
                            .padding(horizontal = 24.dp, vertical = if (isActive) 12.dp else 7.dp)
                            .then(
                                if (isActive) {
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x2206B6D4))
                                        .padding(vertical = 4.dp)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lyric.text,
                            color = textColor,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isActive) 28.sp else 22.sp
                        )
                    }
                }
            }

            // Top control buttons (AI Transcribe & Manual Edit)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Transcribe button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xD9131826))
                        .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onAiTranscribeClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Lyrics",
                            tint = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هوش مصنوعی",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Edit Button
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xD9131826), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Lyrics",
                        tint = NeonPink,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        var editedText by remember { mutableStateOf(lrcContent ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "ویرایش متن ترانه (قالب LRC یا ساده)",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "می‌توانید کدهای زمان‌دار مانند [00:15.00]متن یا خط‌های معمولی را وارد کنید:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        placeholder = {
                            Text("[00:00.00] متن خط اول\n[00:05.50] متن خط دوم")
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveLyrics(editedText)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}
