package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.VisualizerStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    style: VisualizerStyle,
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp)
) {
    val transition = rememberInfiniteTransition(label = "vis_trans")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (style) {
            VisualizerStyle.SPECTRUM_BARS -> {
                val barCount = 32
                val barWidth = (w / barCount) * 0.65f
                val spacing = (w / barCount) * 0.35f
                val gradient = Brush.verticalGradient(
                    colors = listOf(NeonPink, NeonViolet, NeonCyan)
                )

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing) + spacing / 2
                    val multiplier = if (isPlaying) {
                        (sin(phase * 2f + i * 0.45f) * 0.4f +
                         cos(phase * 1.5f + i * 0.25f) * 0.4f + 0.2f).coerceIn(0.12f, 0.95f)
                    } else {
                        0.08f
                    }
                    val barHeight = h * multiplier
                    val y = h - barHeight

                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }

            VisualizerStyle.WAVE_NEON -> {
                val path1 = Path()
                val path2 = Path()
                val points = 60

                path1.moveTo(0f, h / 2)
                path2.moveTo(0f, h / 2)

                for (i in 0..points) {
                    val x = (w / points) * i
                    val waveAmp = if (isPlaying) h * 0.35f else h * 0.04f
                    val y1 = h / 2 + sin(phase + i * 0.22f) * waveAmp
                    val y2 = h / 2 + cos(phase * 1.3f + i * 0.18f) * (waveAmp * 0.75f)

                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }

                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(listOf(NeonCyan, NeonViolet, NeonPink)),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(listOf(NeonPink, NeonViolet, NeonCyan)),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            VisualizerStyle.RADIAL_PULSE -> {
                val centerX = w / 2
                val centerY = h / 2
                val baseRadius = (minOf(w, h) * 0.28f)

                val pulse = if (isPlaying) sin(phase * 2) * 12f else 0f
                val radius = baseRadius + pulse

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonViolet.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius * 1.5f
                    ),
                    radius = radius * 1.4f,
                    center = Offset(centerX, centerY)
                )

                drawCircle(
                    color = NeonCyan,
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Particle dots around circle
                val particles = 12
                for (p in 0 until particles) {
                    val angle = (phase + (2 * PI / particles) * p).toFloat()
                    val px = centerX + cos(angle) * (radius + 16f)
                    val py = centerY + sin(angle) * (radius + 16f)
                    drawCircle(
                        color = NeonPink,
                        radius = 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
