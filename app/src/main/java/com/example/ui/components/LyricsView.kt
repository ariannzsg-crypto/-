package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Composable
fun LyricsView(
    rawLyrics: String?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val parsedLines = remember(rawLyrics) { parseLyrics(rawLyrics) }
    val listState = rememberLazyListState()

    // Find active line index
    val activeIndex = remember(parsedLines, currentPositionMs) {
        val idx = parsedLines.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(activeIndex) {
        if (parsedLines.isNotEmpty() && activeIndex in parsedLines.indices) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard.copy(alpha = 0.85f))
            .padding(16.dp)
    ) {
        if (parsedLines.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "متن ترانه برای این آهنگ ثبت نشده است",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("افزودن متن آهنگ (Add Lyrics)")
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                itemsIndexed(parsedLines) { index, line ->
                    val isCurrent = index == activeIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isCurrent) NeonCyan else TextMuted,
                        label = "lyric_color"
                    )

                    Text(
                        text = line.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isCurrent) 19.sp else 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                            .clickable {
                                if (line.timeMs > 0) onSeekTo(line.timeMs)
                            }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            // Edit button floating at bottom right
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHighlight.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Lyrics",
                    tint = NeonCyan
                )
            }
        }

        if (showEditDialog) {
            EditLyricsDialog(
                initialText = rawLyrics ?: "",
                onDismiss = { showEditDialog = false },
                onConfirm = {
                    onSaveLyrics(it)
                    showEditDialog = false
                }
            )
        }
    }
}

private fun parseLyrics(raw: String?): List<LyricLine> {
    if (raw.isNullOrBlank()) return emptyList()
    val lines = raw.lines()
    val result = mutableListOf<LyricLine>()
    val regex = Regex("""\[(\d{2}):(\d{2})\.?(\d{0,3})\](.*)""")

    for (line in lines) {
        val match = regex.find(line.trim())
        if (match != null) {
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val millisPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            val timeMs = (min * 60 + sec) * 1000 + millisPart
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                result.add(LyricLine(timeMs, text))
            }
        } else if (line.isNotBlank()) {
            // plain text line without timestamp
            result.add(LyricLine(-1L, line.trim()))
        }
    }
    return result
}

@Composable
fun EditLyricsDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "ویرایش متن آهنگ / Edit Lyrics", color = TextPrimary)
        },
        text = {
            Column {
                Text(
                    text = "می‌توانید متن ساده یا متن هماهنگ با فرمت [00:15.00] وارد کنید:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text("[00:05.00] کلمات ترانه...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
            ) {
                Text("ذخیره / Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف / Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceCard
    )
}
