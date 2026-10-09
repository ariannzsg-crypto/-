package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.VisualizerStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    amplitudes: FloatArray,
    isPlaying: Boolean,
    style: VisualizerStyle,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (style) {
            VisualizerStyle.SPECTRUM_BARS -> {
                val numBars = amplitudes.size.coerceAtLeast(8)
                val spacing = 6f
                val barWidth = (width - (numBars - 1) * spacing) / numBars

                val gradient = Brush.verticalGradient(
                    colors = listOf(NeonPink, NeonViolet, NeonCyan),
                    startY = 0f,
                    endY = height
                )

                for (i in 0 until numBars) {
                    val amp = if (isPlaying) amplitudes.getOrElse(i) { 0.1f } else 0.08f
                    val barHeight = (height * amp * 0.95f).coerceAtLeast(8f)
                    val x = i * (barWidth + spacing)
                    val y = height - barHeight

                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }

            VisualizerStyle.WAVE_NEON -> {
                val path = Path()
                val midY = height / 2f
                val points = 30
                val stepX = width / (points - 1)

                val primaryBrush = Brush.horizontalGradient(
                    colors = listOf(NeonCyan, NeonViolet, NeonPink)
                )

                path.moveTo(0f, midY)
                for (i in 0 until points) {
                    val x = i * stepX
                    val ampIndex = (i % amplitudes.size)
                    val amp = if (isPlaying) amplitudes[ampIndex] else 0.05f
                    val waveOffset = sin((i * 0.4f) + phase) * (height * 0.35f * amp)
                    val y = midY + waveOffset
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    brush = primaryBrush,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Secondary soft harmonic wave
                val glowPath = Path()
                glowPath.moveTo(0f, midY)
                for (i in 0 until points) {
                    val x = i * stepX
                    val ampIndex = ((i + 2) % amplitudes.size)
                    val amp = if (isPlaying) amplitudes[ampIndex] else 0.03f
                    val waveOffset = cos((i * 0.3f) - phase) * (height * 0.22f * amp)
                    val y = midY + waveOffset
                    if (i == 0) glowPath.moveTo(x, y) else glowPath.lineTo(x, y)
                }

                drawPath(
                    path = glowPath,
                    color = NeonCyan.copy(alpha = 0.5f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            VisualizerStyle.CIRCULAR_PULSE -> {
                val center = Offset(width / 2f, height / 2f)
                val baseRadius = (minOf(width, height) / 3.2f)

                val avgAmp = if (isPlaying) amplitudes.average().toFloat() else 0.08f

                // Outer neon pulse ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, NeonViolet.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.6f)),
                        center = center,
                        radius = baseRadius * (1f + avgAmp * 0.8f)
                    ),
                    radius = baseRadius * (1f + avgAmp * 0.8f),
                    center = center,
                    style = Stroke(width = 5f)
                )

                // Circular frequency spikes
                val spikes = 24
                for (i in 0 until spikes) {
                    val angle = (i.toFloat() / spikes) * 2f * PI.toFloat() + phase * 0.2f
                    val amp = if (isPlaying) amplitudes[i % amplitudes.size] else 0.1f
                    val spikeLength = (baseRadius * 0.5f * amp).coerceAtLeast(10f)

                    val startX = center.x + cos(angle) * baseRadius
                    val startY = center.y + sin(angle) * baseRadius
                    val endX = center.x + cos(angle) * (baseRadius + spikeLength)
                    val endY = center.y + sin(angle) * (baseRadius + spikeLength)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(NeonViolet, NeonPink),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerStyle.VINYL_RECORD -> {
                // Background subtle ambient glow
                val center = Offset(width / 2f, height / 2f)
                val avgAmp = if (isPlaying) amplitudes.average().toFloat() else 0.05f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonViolet.copy(alpha = 0.25f * avgAmp), Color.Transparent),
                        center = center,
                        radius = width * 0.45f
                    ),
                    center = center,
                    radius = width * 0.45f
                )
            }
        }
    }
}
