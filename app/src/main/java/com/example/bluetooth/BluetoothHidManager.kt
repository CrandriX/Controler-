package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

enum class ConnectionStatus {
    UNSUPPORTED,
    DISABLED,
    DISCONNECTED,
    REGISTERED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class BluetoothHidManager(private val context: Context) {

    private val tag = "BluetoothHidManager"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothHidDevice: BluetoothHidDevice? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val executor = Executors.newSingleThreadExecutor()

    private val HID_DESCRIPTOR = byteArrayOf(
        // GAMEPAD
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Gamepad)
        0xa1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        
        // Buttons (A, B, X, Y, L1, R1, L2, R2, Select, Start, L3, R3, Guide)
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (1)
        0x29.toByte(), 0x10.toByte(), //   Usage Maximum (16)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x10.toByte(), //   Report Count (16)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        
        // D-Pad / Hat Switch
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(), //   Usage (Hat Switch)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x07.toByte(), //   Logical Maximum (7)
        0x35.toByte(), 0x00.toByte(), //   Physical Minimum (0)
        0x46.toByte(), 0x3b.toByte(), 0x01.toByte(), //   Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(), //   Unit (Degrees)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x42.toByte(), //   Input (Data, Variable, Absolute, Null State)
        
        // Padding for D-Pad (4 bits)
        0x75.toByte(), 0x04.toByte(), //   Report Size (4)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x03.toByte(), //   Input (Constant, Variable, Absolute)
        
        // Left & Right Joysticks (4 axes: LX, LY, RX, RY)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xa1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x09.toByte(), 0x32.toByte(), //     Usage (Z)
        0x09.toByte(), 0x35.toByte(), //     Usage (Rz)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0xff.toByte(), //     Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x04.toByte(), //     Report Count (4)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute)
        0xc0.toByte(),                //   End Collection
        0xc0.toByte(),                // End Collection

        // TOUCHPAD (MOUSE EMULATION)
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xa1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x02.toByte(), //   Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xa1.toByte(), 0x00.toByte(), //   Collection (Physical)
        
        // Buttons (Left, Right, Middle click)
        0x05.toByte(), 0x09.toByte(), //     Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //     Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(), //     Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //     Report Size (1)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute)
        
        // Padding for Buttons (5 bits constant)
        0x75.toByte(), 0x05.toByte(), //     Report Size (5)
        0x95.toByte(), 0x01.toByte(), //     Report Count (1)
        0x81.toByte(), 0x03.toByte(), //     Input (Constant, Variable, Absolute)
        
        // Relative mouse pointer movements
        0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7f.toByte(), //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x02.toByte(), //     Report Count (2)
        0x81.toByte(), 0x06.toByte(), //     Input (Data, Variable, Relative)
        0xc0.toByte(),                //   End Collection
        0xc0.toByte()                 // End Collection
    )

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(tag, "Service Connected: Bluetooth HID Device proxy acquired.")
                bluetoothHidDevice = proxy as? BluetoothHidDevice
                registerApp()
                updatePairedDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(tag, "Service Disconnected: Bluetooth HID Device proxy released.")
                bluetoothHidDevice = null
                _isRegistered.value = false
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(tag, "onAppStatusChanged: registered = $registered")
            _isRegistered.value = registered
            if (registered) {
                _connectionStatus.value = ConnectionStatus.REGISTERED
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(tag, "onConnectionStateChanged: device = ${device?.name}, state = $state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice.value = device
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedDevice.value = null
                    _connectionStatus.value = if (_isRegistered.value) ConnectionStatus.REGISTERED else ConnectionStatus.DISCONNECTED
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(tag, "onGetReport: host requested state of report ID $id")
            // Send default report data back if needed, but normally gamepad/mouse updates are purely pushed (Interrupt channel)
            if (device != null && id == 1.toByte()) {
                this@BluetoothHidManager.bluetoothHidDevice?.sendReport(device, 1, byteArrayOf(0, 0, 8, 127, 127, 127, 127))
            } else if (device != null && id == 2.toByte()) {
                this@BluetoothHidManager.bluetoothHidDevice?.sendReport(device, 2, byteArrayOf(0, 0, 0))
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(tag, "onSetReport: received set report from device")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            // Emulated device usually pushes data to host, but does not receive interrupt inputs.
        }
    }

    init {
        initializeProxy()
    }

    fun initializeProxy() {
        if (bluetoothAdapter == null) {
            _connectionStatus.value = ConnectionStatus.UNSUPPORTED
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _connectionStatus.value = ConnectionStatus.DISABLED
            return
        }

        try {
            bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        } catch (e: Exception) {
            Log.e(tag, "Error accessing Bluetooth HID Device API: ${e.message}")
            _connectionStatus.value = ConnectionStatus.UNSUPPORTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val hidDev = bluetoothHidDevice ?: return
        if (_isRegistered.value) return

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Bluetooth Game Controller",
            "Sleek virtual touchpad and gamepad peripheral combo",
            "AI Studio",
            0xC0.toByte(), // Combo peripheral (Keyboard/Mouse/Pointer helper)
            HID_DESCRIPTOR
        )

        try {
            val success = hidDev.registerApp(sdpSettings, null, null, executor, callback)
            Log.d(tag, "registerApp status: $success")
            if (!success) {
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception registering SDP settings: ${e.message}")
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun updatePairedDevices() {
        val adapter = bluetoothAdapter ?: return
        try {
            if (adapter.isEnabled) {
                _pairedDevices.value = adapter.bondedDevices.toList()
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Permission error fetching paired devices: ${e.message}")
        } catch (e: Exception) {
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        val hidDev = bluetoothHidDevice ?: return false
        _connectionStatus.value = ConnectionStatus.CONNECTING
        return try {
            val outcome = hidDev.connect(device)
            Log.d(tag, "connect to ${device.name} outcome: $outcome")
            outcome
        } catch (e: SecurityException) {
            Log.e(tag, "Missing connection permission to connect: ${e.message}")
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect: ${e.message}")
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val hidDev = bluetoothHidDevice ?: return
        val connected = _connectedDevice.value ?: return
        try {
            hidDev.disconnect(connected)
            _connectedDevice.value = null
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun unregister() {
        val hidDev = bluetoothHidDevice ?: return
        try {
            hidDev.unregisterApp()
            _isRegistered.value = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering application: ${e.message}")
        }
    }

    /**
     * Sends Gamepad updates to the host
     * 7-byte report:
     * - byte 0: Buttons 1..8
     * - byte 1: Buttons 9..16
     * - byte 2: Hat Switch (D-Pad, 0-7, 8 for neutral) + Padding
     * - byte 3: Left stick X (0..255)
     * - byte 4: Left stick Y (0..255)
     * - byte 5: Right stick X (0..255)
     * - byte 6: Right stick Y (0..255)
     */
    @SuppressLint("MissingPermission")
    fun sendGamepadReport(
        buttonsMaskLow: Int, // bits 0-7: A, B, X, Y, L1, R1, L2, R2
        buttonsMaskHigh: Int, // bits 8-15: Select, Start, L3, R3, Guide, Select buttons
        dpadHat: Byte,      // 0-7, or 8 (neutral)
        lx: Int, ly: Int,   // 0-255
        rx: Int, ry: Int    // 0-255
    ): Boolean {
        val hidDev = bluetoothHidDevice ?: return false
        val device = _connectedDevice.value ?: return false

        val reportData = byteArrayOf(
            (buttonsMaskLow and 0xFF).toByte(),
            (buttonsMaskHigh and 0xFF).toByte(),
            (dpadHat.toInt() and 0x0F).toByte(), // 4 bits for Hat, remaining 4 padding is 0
            lx.toByte(),
            ly.toByte(),
            rx.toByte(),
            ry.toByte()
        )

        return try {
            hidDev.sendReport(device, 1, reportData)
        } catch (e: Exception) {
            Log.e(tag, "Error sending gamepad report: ${e.message}")
            false
        }
    }

    /**
     * Sends Touchpad (relative mouse) updates to the host
     * 3-byte report:
     * - byte 0: Mouse buttons status (bit 0=Left, bit 1=Right, bit 2=Middle) + Padding
     * - byte 1: dX (-127 to 127)
     * - byte 2: dY (-127 to 127)
     */
    @SuppressLint("MissingPermission")
    fun sendMouseReport(buttons: Byte, dx: Int, dy: Int): Boolean {
        val hidDev = bluetoothHidDevice ?: return false
        val device = _connectedDevice.value ?: return false

        // Clamp values to valid HID range (-127..127)
        val clampedDx = dx.coerceIn(-127, 127).toByte()
        val clampedDy = dy.coerceIn(-127, 127).toByte()

        val reportData = byteArrayOf(
            buttons,
            clampedDx,
            clampedDy
        )

        return try {
            hidDev.sendReport(device, 2, reportData)
        } catch (e: Exception) {
            Log.e(tag, "Error sending mouse report: ${e.message}")
            false
        }
    }
}
