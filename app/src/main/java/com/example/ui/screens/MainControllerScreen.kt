package com.example.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.ConnectionStatus
import com.example.model.Profile
import com.example.ui.components.*
import com.example.ui.viewmodel.ControllerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainControllerScreen(
    viewModel: ControllerViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val connStatus by viewModel.bluetoothHidManager.connectionStatus.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.bluetoothHidManager.connectedDevice.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.bluetoothHidManager.pairedDevices.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var showProfileCreator by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }
    var joySensInput by remember { mutableFloatStateOf(1.2f) }
    var touchSensInput by remember { mutableFloatStateOf(1.5f) }

    // Sci-fi themed master canvas background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0B0D), // Pure space black
                        Color(0xFF050507)  // Deep rich dark bottom
                    )
                )
            )
    ) {
        if (!hasPermissions) {
            // Permission Block View
            PermissionEducationalView(onRequestPermissions)
        } else {
            if (isLandscape) {
                // LANDSCAPE MODE: Fullscreen Interactive Controller Pad
                LandscapeGamepadView(
                    viewModel = viewModel,
                    connStatus = connStatus,
                    connectedDevice = connectedDevice,
                    activeProfile = activeProfile,
                    onDisconnect = { viewModel.disconnectDevice() }
                )
            } else {
                // PORTRAIT MODE: Connection Center, Profile Editor & Walkthrough Guide
                PortraitDashboardView(
                    connStatus = connStatus,
                    connectedDevice = connectedDevice,
                    pairedDevices = pairedDevices,
                    profiles = profiles,
                    activeProfile = activeProfile,
                    viewModel = viewModel,
                    showProfileCreator = showProfileCreator,
                    profileNameInput = profileNameInput,
                    joySensInput = joySensInput,
                    touchSensInput = touchSensInput,
                    onNameChange = { profileNameInput = it },
                    onJoySensChange = { joySensInput = it },
                    onTouchSensChange = { touchSensInput = it },
                    onToggleCreator = {
                        profileNameInput = ""
                        joySensInput = 1.2f
                        touchSensInput = 1.5f
                        showProfileCreator = !showProfileCreator
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionEducationalView(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Bluetooth Needed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "BLUETOOTH PERMISSIONS REQUIRED",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To turn your phone into a physical Bluetooth controller for Xbox, Playstation, PC, and local games, please grant access to nearby Bluetooth device connection permissions.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 400.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .height(50.dp)
                .testTag("grant_permissions_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("AUTHORIZE WIRELESS LINK")
        }
    }
}

@Composable
fun LandscapeGamepadView(
    viewModel: ControllerViewModel,
    connStatus: ConnectionStatus,
    connectedDevice: BluetoothDevice?,
    activeProfile: Profile?,
    onDisconnect: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (connStatus) {
            ConnectionStatus.CONNECTED -> Color(0xFF10B981) // Neon Green
            ConnectionStatus.CONNECTING -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFFEF4444) // Red
        },
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(8.dp)
    ) {
        // TOP HEADER BAR (Bumpers L1/R1 + Stats Status)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // L1 Bumper
            BumperButton(
                label = "LB (L1)",
                onClickState = { active -> viewModel.setButtonPressed(5, active) },
                modifier = Modifier.testTag("bumper_l1")
            )

            // Sci-Fi Status Screen Module
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (connStatus) {
                        ConnectionStatus.CONNECTED -> "ON-AIR: ${connectedDevice?.name ?: "HOST"}"
                        ConnectionStatus.CONNECTING -> "LINKING HOST..."
                        else -> "OFFLINE - TILT PORTRAIT TO CONNECT"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (activeProfile != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "•  PROFILE: ${activeProfile.name.uppercase()}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // R1 Bumper
            BumperButton(
                label = "RB (R1)",
                onClickState = { active -> viewModel.setButtonPressed(6, active) },
                modifier = Modifier.testTag("bumper_r1")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // THE CORE CONTROLLER PANELS (Joysticks, DPad, Central Touchpad, ABXY)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: Joystick Left & D-Pad Arrangement
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Joystick(
                    size = 130.dp,
                    onValueChange = { x, y -> viewModel.setLeftJoystick(x, y) },
                    modifier = Modifier.testTag("left_joystick")
                )

                DPad(
                    size = 120.dp,
                    onDirectionChange = { dir -> viewModel.setDPadDirection(dir) },
                    modifier = Modifier.testTag("dpad_controller")
                )
            }

            // MIDDLE PANEL: Precision Trackzone & Menu Buttons
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // L2/R2 Dynamic Analog Slider Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TriggerSliderButton(label = "LT (L2)", onClickState = { active -> viewModel.setButtonPressed(7, active) })
                    TriggerSliderButton(label = "RT (R2)", onClickState = { active -> viewModel.setButtonPressed(8, active) })
                }

                // Spacious Drag Touchpad
                Touchpad(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .testTag("central_touchpad"),
                    onMove = { dx, dy -> viewModel.sendTouchpadMove(dx, dy) },
                    onButtonState = { bit, pressed -> viewModel.setMouseButtonPressed(bit, pressed) }
                )

                // Option pills (Select, Start, Guide)
                ConsoleOptionsCluster(
                    onSelectState = { active -> viewModel.setButtonPressed(9, active) },
                    onStartState = { active -> viewModel.setButtonPressed(10, active) },
                    onGuideState = { active -> viewModel.setButtonPressed(13, active) },
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("options_cluster")
                )
            }

            // RIGHT PANEL: ABXY Diamond button deck & Right Joystick
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                ABXYCluster(
                    size = 120.dp,
                    onButtonState = { index, pressed -> viewModel.setButtonPressed(index, pressed) },
                    modifier = Modifier.testTag("abxy_cluster")
                )

                Joystick(
                    size = 130.dp,
                    onValueChange = { x, y -> viewModel.setRightJoystick(x, y) },
                    modifier = Modifier.testTag("right_joystick")
                )
            }
        }
    }
}

@Composable
fun TriggerSliderButton(
    label: String,
    onClickState: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else Color(0xFF14161A)
            )
            .border(1.dp, if (isPressed) MaterialTheme.colorScheme.primary else Color(0xFF1E293B), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
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
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitDashboardView(
    connStatus: ConnectionStatus,
    connectedDevice: BluetoothDevice?,
    pairedDevices: List<BluetoothDevice>,
    profiles: List<Profile>,
    activeProfile: Profile?,
    viewModel: ControllerViewModel,
    showProfileCreator: Boolean,
    profileNameInput: String,
    joySensInput: Float,
    touchSensInput: Float,
    onNameChange: (String) -> Unit,
    onJoySensChange: (Float) -> Unit,
    onTouchSensChange: (Float) -> Unit,
    onToggleCreator: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "JOYSTREAM CONTROLLER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0B0D).copy(alpha = 0.95f),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Bluetooth Connection Status Panel
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ConnectionStatusWidget(
                    status = connStatus,
                    connectedDevice = connectedDevice,
                    onRetry = { viewModel.retryBluetoothProxy() },
                    onDisconnect = { viewModel.disconnectDevice() }
                )
            }

            // 2. Quick Tilt prompt block
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Rotate Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "READY FOR WARFARE?",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Simply tilt your device sideways (landscape) to launch the high-performance gamepad deck!",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 3. Paired Gaming Devices List
            item {
                SectionTitle(text = "SELECT TARGET DEVICE")
            }

            if (connStatus == ConnectionStatus.UNSUPPORTED) {
                item {
                    Text(
                        text = "Bluetooth HID Device Profile is unsupported on this system or container. Pair natively on real devices to play.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else if (pairedDevices.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No bonded host devices found.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.scanPairedDevices() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Bonded Hosts", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(pairedDevices) { device ->
                    DeviceRow(
                        device = device,
                        isConnected = connectedDevice?.address == device.address,
                        status = connStatus,
                        onConnect = { viewModel.connectDevice(device) }
                    )
                }
            }

            // 4. Controller Configuration Profiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(text = "ACTIVE CONFIG PROFILE")
                    IconButton(onClick = onToggleCreator) {
                        Icon(
                            imageVector = if (showProfileCreator) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Optional Inline Creator card
            if (showProfileCreator) {
                item {
                    ProfileCreatorForm(
                        name = profileNameInput,
                        joySens = joySensInput,
                        touchSens = touchSensInput,
                        onNameChange = onNameChange,
                        onJoySensChange = onJoySensChange,
                        onTouchSensChange = onTouchSensChange,
                        onSave = {
                            if (profileNameInput.isNotBlank()) {
                                viewModel.createProfile(profileNameInput, joySensInput, touchSensInput)
                                onToggleCreator()
                            }
                        }
                    )
                }
            }

            items(profiles) { profile ->
                ProfileRow(
                    profile = profile,
                    isActive = activeProfile?.id == profile.id,
                    onSelect = { viewModel.selectProfile(profile) },
                    onDelete = { viewModel.deleteProfile(profile) }
                )
            }

            // 5. Cross-platform Guidebook Card
            item {
                GuidebookWidget()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ConnectionStatusWidget(
    status: ConnectionStatus,
    connectedDevice: BluetoothDevice?,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit
) {
    val statusColors = when (status) {
        ConnectionStatus.CONNECTED -> Triple(Color(0xFF10B981), "CONNECTED", "Ready to issue commands")
        ConnectionStatus.CONNECTING -> Triple(Color(0xFFF59E0B), "CONNECTING", "Establishing L2CAP pipeline")
        ConnectionStatus.REGISTERED -> Triple(Color(0xFF3B82F6), "STAGED", "Awaiting connection from host")
        ConnectionStatus.DISABLED -> Triple(Color(0xFFEF4444), "BLUETOOTH OFF", "Please turn on Bluetooth")
        ConnectionStatus.UNSUPPORTED -> Triple(Color(0xFF64748B), "UNSUPPORTED", "HID profile missing on container")
        else -> Triple(Color(0xFFEF4444), "MUTED / OFFLINE", "Wireless controller app inactive")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14161A)
        ),
        border = BorderStroke(1.dp, statusColors.first.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LINK STATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = statusColors.second,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColors.first,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (status == ConnectionStatus.CONNECTED) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                    ) {
                        Text("DISCONNECT", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                } else if (status == ConnectionStatus.DISABLED) {
                    Button(onClick = onRetry) {
                        Text("RETRY LINK", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (status == ConnectionStatus.CONNECTED && connectedDevice != null) {
                    "Pairing established directly with ${connectedDevice.name ?: connectedDevice.address}."
                } else {
                    statusColors.third
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceRow(
    device: BluetoothDevice,
    isConnected: Boolean,
    status: ConnectionStatus,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isConnected && status != ConnectionStatus.CONNECTING) {
                    onConnect()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF14161A)
            else Color(0xFF0E0E10)
        ),
        border = BorderStroke(
            1.dp,
            if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = device.name ?: "Unknown Host",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = device.address,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isConnected) {
                Text(
                    text = "ACTIVE LINK",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "CONNECT",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ProfileCreatorForm(
    name: String,
    joySens: Float,
    touchSens: Float,
    onNameChange: (String) -> Unit,
    onJoySensChange: (Float) -> Unit,
    onTouchSensChange: (Float) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14161A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CREATE CUSTOM BALANCED PROFILE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Profile Name", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Joystick sensitivity slider
            Text(
                "ANALOG JOYSTICK SENSITIVITY: ${(joySens * 100).roundToInt()}%",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Slider(
                value = joySens,
                onValueChange = onJoySensChange,
                valueRange = 0.5f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Touchpad sensitivity slider
            Text(
                "PRECISION TOUCHPAD SENSITIVITY: ${(touchSens * 100).roundToInt()}%",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            Slider(
                value = touchSens,
                onValueChange = onTouchSensChange,
                valueRange = 0.5f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE CONTROLLERS LAYOUT")
            }
        }
    }
}

@Composable
fun ProfileRow(
    profile: Profile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF14161A)
            else Color(0xFF0E0E10)
        ),
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row {
                    Text(
                        text = profile.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (profile.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("DEFAULT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Joy Speed: ${(profile.joystickSensitivity * 100).roundToInt()}%  •  Touch Speed: ${(profile.touchpadSensitivity * 100).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Text(
                        "ACTIVE",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (!profile.isDefault) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun GuidebookWidget() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14161A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CROSS-PLATFORM INSTRUCTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            GuideStep(num = 1, desc = "Ensure your host (PC/Playstation/Xbox/Android) has Bluetooth turned ON and is in discovery search mode.")
            GuideStep(num = 2, desc = "Navigate to your system's Bluetooth pairing preferences and select 'Bluetooth Game Controller'.")
            GuideStep(num = 3, desc = "Confirm pairing. This application registers natively as an official BT HID Keyboard/Mouse/Gamepad Combo standard device.")
            GuideStep(num = 4, desc = "Open custom games or web engines (like Steam, Xbox Game Pass, cloud portals). Controls map instantly like a physical hardware pad!")
        }
    }
}

@Composable
fun GuideStep(num: Int, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num.toString(),
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = desc, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.4f),
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
