package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No Lyrics",
                    tint = TextMuted,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "متن ترانه برای این آهنگ ثبت نشده است",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("افزودن متن ترانه (LRC)")
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 120.dp, bottom = 180.dp),
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

                    val fontSize = if (isActive) 21.sp else 16.sp
                    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(lyric.timestampMs) }
                            .padding(horizontal = 24.dp, vertical = if (isActive) 14.dp else 8.dp)
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

            // Top-right edit button to modify lyrics
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color(0x99131826), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Lyrics",
                    tint = NeonPink
                )
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
