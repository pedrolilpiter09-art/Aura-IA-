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
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformBar(
    isActive: Boolean,
    amplitude: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = (width / barCount) * 0.55f
        val gap = (width - (barCount * barWidth)) / (barCount - 1)

        val brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor,
                secondaryColor.copy(alpha = 0.8f),
                primaryColor.copy(alpha = 0.3f)
            )
        )

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount.toFloat()
            val sineWave = sin((normalizedX * 12f) + phase).toFloat()

            val targetHeightFraction = if (isActive) {
                val waveHeight = (0.2f + (0.3f * sineWave.coerceAtLeast(0f))) + (amplitude * 0.7f * (0.5f + (i % 5) * 0.1f))
                waveHeight.coerceIn(0.12f, 1.0f)
            } else {
                (0.08f + (0.04f * sin((i * 0.5f) + phase))).coerceIn(0.05f, 0.2f)
            }

            val barHeight = height * targetHeightFraction
            val x = i * (barWidth + gap)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
