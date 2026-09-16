package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioAmplitude: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    orbSize: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_fluid_motion")

    // Smooth continuous mesh rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isListening -> 3500
                    isProcessing -> 1600
                    isSpeaking -> 4500
                    else -> 9000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse outer orbit
    val counterAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_angle"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isListening -> 450
                    isSpeaking -> 700
                    isProcessing -> 350
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Harmonic ring ripple
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1200 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    val dynamicScale = (breathingPulse + (audioAmplitude * 0.45f)).coerceIn(0.85f, 1.55f)

    Box(
        modifier = modifier
            .size(orbSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = orbSize / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.58f

            // 1. Ambient Background Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isListening) 0.35f else 0.18f),
                        secondaryColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.0f * dynamicScale
                ),
                radius = baseRadius * 2.0f * dynamicScale,
                center = center
            )

            // 2. Active Pulsing Echo Rings
            if (isListening || isSpeaking || isProcessing) {
                val rippleAlpha = (1f - (rippleProgress - 0.4f) / 0.95f).coerceIn(0f, 1f)
                drawCircle(
                    color = primaryColor.copy(alpha = rippleAlpha * 0.55f),
                    radius = baseRadius * rippleProgress * (1f + audioAmplitude * 0.3f),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = secondaryColor.copy(alpha = rippleAlpha * 0.35f),
                    radius = baseRadius * (rippleProgress * 0.85f) * (1f + audioAmplitude * 0.3f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 3. Ethereal Glass Halo Orbit Ring
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.7f),
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.85f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.18f * dynamicScale,
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 18f, 12f, 18f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 4. Counter-rotating Delicate Tech Arc
            rotate(counterAngle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.8f),
                            primaryColor.copy(alpha = 0.2f),
                            secondaryColor.copy(alpha = 0.7f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    radius = baseRadius * 0.98f * dynamicScale,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(45f, 25f), 0f)
                    )
                )
            }

            // 5. Luminescent Multi-layered Plasma Core
            val coreRadius = baseRadius * 0.76f * dynamicScale
            val plasmaBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isListening) 0.98f else 0.90f),
                    primaryColor.copy(alpha = 0.92f),
                    secondaryColor.copy(alpha = 0.75f),
                    primaryColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(
                    center.x + (cos(Math.toRadians(rotationAngle.toDouble())).toFloat() * 6.dp.toPx()),
                    center.y + (sin(Math.toRadians(rotationAngle.toDouble())).toFloat() * 6.dp.toPx())
                ),
                radius = coreRadius * 1.25f
            )

            drawCircle(
                brush = plasmaBrush,
                radius = coreRadius,
                center = center
            )

            // 6. Floating Energy Spark Nodes
            val particleCount = 6
            for (i in 0 until particleCount) {
                val pAngle = Math.toRadians(((rotationAngle * 1.3) + (i * (360.0 / particleCount))))
                val pRadius = baseRadius * (0.88f + (sin(pAngle * 2).toFloat() * 0.12f)) * dynamicScale
                val px = center.x + (pRadius * cos(pAngle)).toFloat()
                val py = center.y + (pRadius * sin(pAngle)).toFloat()

                drawCircle(
                    color = if (i % 2 == 0) primaryColor else Color.White,
                    radius = (2.2f + (audioAmplitude * 2.5f)).dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // 7. Center Quantum Core Beacon
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = center
            )
        }
    }
}

