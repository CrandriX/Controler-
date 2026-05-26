package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onValueChange: (x: Float, y: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }

    val sizePx = with(LocalDensity.current) { size.toPx() }
    val outerRadius = sizePx / 2
    val innerRadius = outerRadius * 0.4f // Joystick knob size

    // Themes
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        coroutineScope.launch {
                            onValueChange(0f, 0f)
                            launch { dragX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                            launch { dragY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            onValueChange(0f, 0f)
                            launch { dragX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                            launch { dragY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newX = dragX.value + dragAmount.x
                            val newY = dragY.value + dragAmount.y
                            val distance = sqrt(newX * newX + newY * newY)

                            // Clamp range inside raw outer circle boundary
                            val limit = outerRadius - innerRadius
                            if (distance <= limit) {
                                dragX.snapTo(newX)
                                dragY.snapTo(newY)
                            } else {
                                val ratio = limit / distance
                                dragX.snapTo(newX * ratio)
                                dragY.snapTo(newY * ratio)
                            }

                            // Normalize offsets to -1f .. 1f for the controller output
                            val normalizedX = dragX.value / limit
                            val normalizedY = dragY.value / limit
                            onValueChange(normalizedX, normalizedY)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(sizePx / 2, sizePx / 2)

            // 1. Draw Outer Ring bounds (Socket shadow and deep metallic container build)
            drawCircle(
                color = Color(0xFF090A0C), // Deep charcoal background inner socket
                center = center,
                radius = outerRadius
            )

            // Heavy metallic outer rim wall matching border-4 border-slate-800
            drawCircle(
                color = Color(0xFF1E293B), // Slate-800 metallic frame
                center = center,
                radius = outerRadius,
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Subtler inner cross guides
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(center.x - outerRadius * 0.9f, center.y),
                end = Offset(center.x + outerRadius * 0.9f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(center.x, center.y - outerRadius * 0.9f),
                end = Offset(center.x, center.y + outerRadius * 0.9f),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Draw active thumb stick vector (Tactile connector link)
            val knobCenter = Offset(center.x + dragX.value, center.y + dragY.value)
            if (dragX.value != 0f || dragY.value != 0f) {
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = center,
                    end = knobCenter,
                    strokeWidth = 3.5.dp.toPx()
                )
            }

            // 3. Draw tactile thumb knob (bg-gradient-to-br from-slate-700 to-slate-900 border-slate-600)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF475569), // slate-600/700 highlights
                        Color(0xFF1E293B), // slate-800 base
                        Color(0xFF0F172A)  // slate-900 core shadow
                    ),
                    center = knobCenter,
                    radius = innerRadius
                ),
                center = knobCenter,
                radius = innerRadius
            )

            // Thin high-end tactile ridge border on knob
            drawCircle(
                color = Color(0xFF475569), // slate-600
                center = knobCenter,
                radius = innerRadius,
                style = Stroke(width = 1.25.dp.toPx())
            )

            // Inside knob center indentation cap (physical depression zone)
            drawCircle(
                color = Color(0xFF0F172A), // slate-900 deep center cavity
                center = knobCenter,
                radius = innerRadius * 0.45f
            )

            drawCircle(
                color = Color(0xFF1E293B), // ridge for center cavity
                center = knobCenter,
                radius = innerRadius * 0.45f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic indicator glow dot (Cyan active connection color)
            val glowColor = if (dragX.value != 0f || dragY.value != 0f) primaryColor else primaryColor.copy(alpha = 0.4f)
            drawCircle(
                color = glowColor,
                center = knobCenter,
                radius = 3.dp.toPx()
            )
        }
    }
}
