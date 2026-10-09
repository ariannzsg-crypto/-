package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.VinylCenterGold
import com.example.ui.theme.VinylGrooveDark

@Composable
fun VinylTurntable(
    song: Song?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Continuous rotation for vinyl when playing
    val infiniteTransition = rememberInfiniteTransition(label = "turntable_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    // Animated tone-arm angle (rests off-record at -25 deg, plays on record at 8 deg)
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 12f else -25f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 100f),
        label = "tonearm_angle"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vinyl Record Disc
        Box(
            modifier = Modifier
                .size(260.dp)
                .rotate(currentRotation)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF22283A),
                            VinylGrooveDark,
                            Color(0xFF090B10)
                        )
                    )
                )
                .border(2.dp, SurfaceHighlight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl grooves rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radii = listOf(40f, 60f, 75f, 90f, 105f, 115f)
                radii.forEach { r ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = (size.width / 2f) * (r / 130f),
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Vinyl shine reflection sheen
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    center = center,
                    radius = size.width / 2f
                )
            }

            // Center Album Art Label
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, NeonViolet, CircleShape)
                    .background(SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (song?.albumArtRes != null) {
                    Image(
                        painter = painterResource(id = song.albumArtRes),
                        contentDescription = "Cover Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (song?.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Cover Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Spindle Hole
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090B10))
                        .border(2.dp, VinylCenterGold, CircleShape)
                )
            }
        }

        // Tonearm pivot base (top right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = (-4).dp)
        ) {
            // Tonearm arm swinging
            Box(
                modifier = Modifier
                    .rotate(tonearmAngle)
                    .offset(x = (-16).dp, y = 20.dp)
            ) {
                Canvas(modifier = Modifier.size(width = 70.dp, height = 130.dp)) {
                    // Arm rod
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFF334155))
                        ),
                        start = Offset(60f, 0f),
                        end = Offset(15f, 110f),
                        strokeWidth = 6f
                    )

                    // Head cartridge
                    drawRoundRect(
                        color = NeonViolet,
                        topLeft = Offset(5f, 105f),
                        size = androidx.compose.ui.geometry.Size(22f, 18f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )

                    // Needle tip
                    drawLine(
                        color = Color.White,
                        start = Offset(10f, 123f),
                        end = Offset(10f, 130f),
                        strokeWidth = 3f
                    )
                }
            }

            // Pivot Mount base
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
                    .border(2.dp, Color(0xFF94A3B8), CircleShape)
            )
        }
    }
}
