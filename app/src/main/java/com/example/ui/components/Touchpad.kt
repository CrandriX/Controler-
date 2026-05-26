package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Touchpad(
    modifier: Modifier = Modifier,
    onMove: (dx: Float, dy: Float) -> Unit,
    onButtonState: (bit: Int, pressed: Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    // Track active touch pointer positions to play visual indicators
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var touchIndicatorAlpha by remember { mutableStateOf(0f) }

    // Left/Right click statuses
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }

    val isTouched = (activeTouchPoint != null && touchIndicatorAlpha > 0f) || isLeftPressed || isRightPressed
    val borderColor = if (isTouched) primaryColor else Color(0xFF1E293B)

    Surface(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF14161A), // Matches Sophisticated Dark high-end pad specs
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Gesture Trackpad area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawBehind {
                        // Drawing subtle dotted grid mesh for premium touchpad texture
                        val dotDistance = 16.dp.toPx()
                        val cols = (size.width / dotDistance).toInt() + 1
                        val rows = (size.height / dotDistance).toInt() + 1
                        for (c in 1 until cols) {
                            for (r in 1 until rows) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = 1.dp.toPx(),
                                    center = Offset(c * dotDistance, r * dotDistance)
                                )
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                coroutineScope.launch {
                                    activeTouchPoint = offset
                                    touchIndicatorAlpha = 0.8f
                                    
                                    // Simulate mouse click on tap
                                    onButtonState(1, true) // Left Click DOWN
                                    delay(50)
                                    onButtonState(1, false) // Left Click UP
                                    
                                    // Fade-out indicator
                                    delay(200)
                                    touchIndicatorAlpha = 0f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                activeTouchPoint = startOffset
                                touchIndicatorAlpha = 0.8f
                            },
                            onDragEnd = {
                                touchIndicatorAlpha = 0f
                            },
                            onDragCancel = {
                                touchIndicatorAlpha = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                activeTouchPoint = change.position
                                onMove(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
            ) {
                // Live pointer indicator ring
                if (activeTouchPoint != null && touchIndicatorAlpha > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = primaryColor.copy(alpha = touchIndicatorAlpha),
                            center = activeTouchPoint!!,
                            radius = 28.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = touchIndicatorAlpha * 0.5f),
                            center = activeTouchPoint!!,
                            radius = 12.dp.toPx()
                        )
                    }
                }

                // Grid Center hint text
                Text(
                    text = "PRECISION TRACKZONE",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Divider before buttons
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(outlineColor.copy(alpha = 0.3f))
            )

            // 2. Left & Right Click physical buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                // Left mouse button clicker
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isLeftPressed) primaryColor.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isLeftPressed = true
                                    onButtonState(1, true) // Left Click (Bit 1 = 0x01)
                                    awaitRelease()
                                    isLeftPressed = false
                                    onButtonState(1, false)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L-CLICK",
                        color = if (isLeftPressed) primaryColor else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Vertical Separator
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(outlineColor.copy(alpha = 0.3f))
                )

                // Right mouse button clicker
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isRightPressed) secondaryColor.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isRightPressed = true
                                    onButtonState(2, true) // Right Click (Bit 2 = 0x02)
                                    awaitRelease()
                                    isRightPressed = false
                                    onButtonState(2, false)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R-CLICK",
                        color = if (isRightPressed) secondaryColor else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
