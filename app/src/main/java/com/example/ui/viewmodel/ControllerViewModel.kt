package com.example.ui.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothHidManager
import com.example.bluetooth.ConnectionStatus
import com.example.data.ProfileRepository
import com.example.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ControllerViewModel(
    private val repository: ProfileRepository,
    val bluetoothHidManager: BluetoothHidManager
) : ViewModel() {

    // Profiles
    val profiles: StateFlow<List<Profile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    // Realtime Controller State
    private var pressedButtonsLow = 0  // 8 bits
    private var pressedButtonsHigh = 0 // 8 bits
    private var dpadHat: Byte = 8      // Neutral

    private val _lx = MutableStateFlow(127)
    private val _ly = MutableStateFlow(127)
    private val _rx = MutableStateFlow(127)
    private val _ry = MutableStateFlow(127)

    val lx: StateFlow<Int> = _lx.asStateFlow()
    val ly: StateFlow<Int> = _ly.asStateFlow()
    val rx: StateFlow<Int> = _rx.asStateFlow()
    val ry: StateFlow<Int> = _ry.asStateFlow()

    // Active Touchpad Buttons
    private var mouseButtons: Byte = 0

    init {
        viewModelScope.launch {
            repository.seedTemplatesIfNeeded()
            // Load default profile
            val default = repository.getDefaultProfile()
            if (default != null) {
                _activeProfile.value = default
            } else {
                repository.allProfiles.collect { list ->
                    if (list.isNotEmpty() && _activeProfile.value == null) {
                        _activeProfile.value = list.firstOrNull { it.isDefault } ?: list.first()
                    }
                }
            }
        }
    }

    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            _activeProfile.value = profile
            repository.setDefaultProfile(profile.id)
        }
    }

    fun createProfile(name: String, joystickSens: Float, touchpadSens: Float) {
        viewModelScope.launch {
            val newProfile = Profile(
                name = name,
                joystickSensitivity = joystickSens,
                touchpadSensitivity = touchpadSens,
                isDefault = false
            )
            repository.insertProfile(newProfile)
        }
    }

    fun updateProfileParams(id: Long, name: String, jSens: Float, tSens: Float) {
        viewModelScope.launch {
            val current = repository.getProfileById(id) ?: return@launch
            val updated = current.copy(
                name = name,
                joystickSensitivity = jSens,
                touchpadSensitivity = tSens
            )
            repository.insertProfile(updated)
            if (_activeProfile.value?.id == id) {
                _activeProfile.value = updated
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_activeProfile.value?.id == profile.id) {
                val list = profiles.value
                val fallback = list.firstOrNull { it.id != profile.id }
                if (fallback != null) {
                    selectProfile(fallback)
                } else {
                    _activeProfile.value = null
                }
            }
        }
    }

    // Refresh devices list
    fun scanPairedDevices() {
        bluetoothHidManager.updatePairedDevices()
    }

    fun connectDevice(device: BluetoothDevice) {
        bluetoothHidManager.connect(device)
    }

    fun disconnectDevice() {
        bluetoothHidManager.disconnect()
    }

    fun retryBluetoothProxy() {
        bluetoothHidManager.initializeProxy()
    }

    /**
     * Set Gamepad Button Pressed State
     * index: 1-16 matching the report descriptor buttons
     */
    fun setButtonPressed(buttonIndex: Int, isPressed: Boolean) {
        val shift = buttonIndex - 1
        if (shift < 8) {
            pressedButtonsLow = if (isPressed) {
                pressedButtonsLow or (1 shl shift)
            } else {
                pressedButtonsLow and (1 shl shift).inv()
            }
        } else {
            val shiftHigh = shift - 8
            pressedButtonsHigh = if (isPressed) {
                pressedButtonsHigh or (1 shl shiftHigh)
            } else {
                pressedButtonsHigh and (1 shl shiftHigh).inv()
            }
        }
        dispatchGamepadState()
    }

    /**
     * Set D-Pad hat direction
     * 0: Up, 1: Up-Right, 2: Right, 3: Down-Right, 4: Down, 5: Down-Left, 6: Left, 7: Up-Left, 8: Release
     */
    fun setDPadDirection(direction: Byte) {
        dpadHat = direction
        dispatchGamepadState()
    }

    /**
     * Set Joystick Inputs (-1.0 to 1.0)
     */
    fun setLeftJoystick(x: Float, y: Float) {
        val sensitivity = _activeProfile.value?.joystickSensitivity ?: 1.0f
        
        // Scale and shift value: x is mapped around center (127), ranging 0 to 255
        val scaledX = (x * sensitivity).coerceIn(-1.0f, 1.0f)
        val scaledY = (y * sensitivity).coerceIn(-1.0f, 1.0f)

        _lx.value = ((scaledX + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
        _ly.value = ((scaledY + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
        
        dispatchGamepadState()
    }

    fun setRightJoystick(x: Float, y: Float) {
        val sensitivity = _activeProfile.value?.joystickSensitivity ?: 1.0f
        
        val scaledX = (x * sensitivity).coerceIn(-1.0f, 1.0f)
        val scaledY = (y * sensitivity).coerceIn(-1.0f, 1.0f)

        _rx.value = ((scaledX + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
        _ry.value = ((scaledY + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
        
        dispatchGamepadState()
    }

    /**
     * Mouse pointer triggers (bit 0: Left, bit 1: Right, bit 2: Middle)
     */
    fun setMouseButtonPressed(buttonBit: Int, isPressed: Boolean) {
        mouseButtons = if (isPressed) {
            (mouseButtons.toInt() or buttonBit).toByte()
        } else {
            (mouseButtons.toInt() and buttonBit.inv()).toByte()
        }
        dispatchMouseState(0, 0)
    }

    /**
     * Trigger a relative touchpad mouse update
     */
    fun sendTouchpadMove(dx: Float, dy: Float) {
        val sensitivity = _activeProfile.value?.touchpadSensitivity ?: 1.2f
        val cleanDx = (dx * sensitivity).roundToInt()
        val cleanDy = (dy * sensitivity).roundToInt()
        
        if (cleanDx != 0 || cleanDy != 0) {
            dispatchMouseState(cleanDx, cleanDy)
        }
    }

    private fun dispatchGamepadState() {
        bluetoothHidManager.sendGamepadReport(
            buttonsMaskLow = pressedButtonsLow,
            buttonsMaskHigh = pressedButtonsHigh,
            dpadHat = dpadHat,
            lx = _lx.value,
            ly = _ly.value,
            rx = _rx.value,
            ry = _ry.value
        )
    }

    private fun dispatchMouseState(dx: Int, dy: Int) {
        bluetoothHidManager.sendMouseReport(mouseButtons, dx, dy)
    }
}

class ControllerViewModelFactory(
    private val repository: ProfileRepository,
    private val bluetoothHidManager: BluetoothHidManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ControllerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ControllerViewModel(repository, bluetoothHidManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
