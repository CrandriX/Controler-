package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DPad(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onDirectionChange: (direction: Byte) -> Unit
) {
    // Keep track of which keys are currently held to support diagonal combinations
    var holdsUp by remember { mutableStateOf(false) }
    var holdsDown by remember { mutableStateOf(false) }
    var holdsLeft by remember { mutableStateOf(false) }
    var holdsRight by remember { mutableStateOf(false) }

    // Re-calculate direction whenever state shifts
    LaunchedEffect(holdsUp, holdsDown, holdsLeft, holdsRight) {
        val dir: Byte = when {
            holdsUp && holdsRight -> 1
            holdsDown && holdsRight -> 3
            holdsDown && holdsLeft -> 5
            holdsUp && holdsLeft -> 7
            holdsUp -> 0
            holdsRight -> 2
            holdsDown -> 4
            holdsLeft -> 6
            else -> 8 // Neutral
        }
        onDirectionChange(dir)
    }

    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF090A0C), CircleShape)
            .border(2.dp, Color(0xFF1E293B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner cosmetic card
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .background(Color(0xFF14161A), CircleShape)
        )

        // UP Button
        DPadArrow(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp),
            onClickState = { active -> holdsUp = active }
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "DPad Up", tint = Color.White)
        }

        // DOWN Button
        DPadArrow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp),
            onClickState = { active -> holdsDown = active }
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "DPad Down", tint = Color.White)
        }

        // LEFT Button
        DPadArrow(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 6.dp),
            onClickState = { active -> holdsLeft = active }
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "DPad Left", tint = Color.White)
        }

        // RIGHT Button
        DPadArrow(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp),
            onClickState = { active -> holdsRight = active }
        ) {
            Icon(Icons.Filled.ArrowForward, contentDescription = "DPad Right", tint = Color.White)
        }

        // Central visual core cap
        Box(
            modifier = Modifier
                .size(size * 0.35f)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF475569), Color(0xFF14161A))
                    ),
                    CircleShape
                )
                .border(1.dp, Color(0xFF1E293B), CircleShape)
        )
    }
}

@Composable
fun DPadArrow(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClickState: (pressed: Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else Color(0xFF1E293B).copy(alpha = 0.6f)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onClickState(true)
                        awaitRelease()
                        isPressed = false
                        onClickState(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@Composable
fun ABXYCluster(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onButtonState: (index: Int, pressed: Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF090A0C), CircleShape)
            .border(2.dp, Color(0xFF1E293B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner visual ring
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .background(Color(0xFF14161A), CircleShape)
        )

        // Y (North button - Index 4)
        ActionRoundButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp),
            label = "Y",
            borderColors = listOf(Color(0xFFFACC15), Color(0xFFCA8A04)), // Yellow Gold
            onClickState = { active -> onButtonState(4, active) }
        )

        // A (South button - Index 1)
        ActionRoundButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp),
            label = "A",
            borderColors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)), // Green Mint
            onClickState = { active -> onButtonState(1, active) }
        )

        // X (West button - Index 3)
        ActionRoundButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 6.dp),
            label = "X",
            borderColors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB)), // Blue Ice
            onClickState = { active -> onButtonState(3, active) }
        )

        // B (East button - Index 2)
        ActionRoundButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-6).dp),
            label = "B",
            borderColors = listOf(Color(0xFFF87171), Color(0xFFDC2626)), // Red Coral
            onClickState = { active -> onButtonState(2, active) }
        )
    }
}

@Composable
fun ActionRoundButton(
    modifier: Modifier = Modifier,
    label: String,
    borderColors: List<Color>,
    onClickState: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) borderColors.first().copy(alpha = 0.25f)
                else Color(0xFF14161A)
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(borderColors),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onClickState(true)
                        awaitRelease()
                        isPressed = false
                        onClickState(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) borderColors.first() else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BumperButton(
    label: String,
    onClickState: (pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .width(100.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPressed) primaryColor.copy(alpha = 0.3f)
                else Color(0xFF1E293B)
            )
            .border(1.5.dp, if (isPressed) primaryColor else Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onClickState(true)
                        awaitRelease()
                        isPressed = false
                        onClickState(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) primaryColor else Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ConsoleOptionsCluster(
    onSelectState: (pressed: Boolean) -> Unit,
    onStartState: (pressed: Boolean) -> Unit,
    onGuideState: (pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Select button (Index 9)
        OptionPillButton(label = "SELECT", onClickState = onSelectState)

        Spacer(modifier = Modifier.width(16.dp))

        // Guide / Home button (Index 13)
        OptionPillButton(
            label = "GUIDE",
            onClickState = onGuideState,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Start button (Index 10)
        OptionPillButton(label = "START", onClickState = onStartState)
    }
}

@Composable
fun OptionPillButton(
    label: String,
    onClickState: (pressed: Boolean) -> Unit,
    color: Color = Color.White.copy(alpha = 0.6f)
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(75.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPressed) color.copy(alpha = 0.25f)
                else Color(0xFF0F172A)
            )
            .border(1.dp, if (isPressed) color else color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onClickState(true)
                        awaitRelease()
                        isPressed = false
                        onClickState(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) color else Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
