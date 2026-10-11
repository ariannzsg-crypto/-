package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet

@Composable
fun VinylTurntable(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier.size(280.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rot")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 28f else 0f,
        animationSpec = tween(600),
        label = "arm"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer Vinyl Disc with Grooves
        Box(
            modifier = Modifier
                .size(260.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .clip(CircleShape)
                .background(Color(0xFF0F1117))
                .border(2.dp, NeonViolet.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2

                // Concentric Grooves
                val grooveColor = Color(0xFF1E2230)
                for (r in (maxRadius * 0.45f).toInt()..(maxRadius * 0.95f).toInt() step 12) {
                    drawCircle(
                        color = grooveColor,
                        radius = r.toFloat(),
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }

                // Vinyl Sheen Gradients
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = maxRadius,
                    center = center
                )
            }

            // Center Album Art Sticker
            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (song?.albumArtRes != null) {
                    Image(
                        painter = painterResource(id = song.albumArtRes),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (song?.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(NeonViolet, NeonCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.album_art_neon),
                            contentDescription = "Default Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090C15))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
            }
        }

        // Tonearm
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(width = 80.dp, height = 140.dp)
                .rotate(tonearmAngle)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pivot = Offset(size.width - 20f, 20f)
                val cartridge = Offset(15f, size.height - 25f)

                // Pivot Base
                drawCircle(color = Color(0xFF64748B), radius = 18f, center = pivot)
                drawCircle(color = NeonCyan, radius = 8f, center = pivot)

                // Arm
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = pivot,
                    end = cartridge,
                    strokeWidth = 6f
                )

                // Cartridge / Needle head
                drawCircle(color = NeonViolet, radius = 9f, center = cartridge)
            }
        }
    }
}
