package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.bluetooth.BluetoothHidManager
import com.example.data.AppDatabase
import com.example.data.ProfileRepository
import com.example.ui.screens.MainControllerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ControllerViewModel
import com.example.ui.viewmodel.ControllerViewModelFactory

class MainActivity : ComponentActivity() {

    private val bluetoothHidManager by lazy { BluetoothHidManager(applicationContext) }

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { ProfileRepository(database.profileDao()) }

    private val viewModel: ControllerViewModel by viewModels {
        ControllerViewModelFactory(repository, bluetoothHidManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        setContent {
            MyApplicationTheme {
                var hasPermissions by remember {
                    mutableStateOf(
                        permissionsToRequest.all {
                            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasPermissions = result.values.all { it }
                    if (hasPermissions) {
                        viewModel.scanPairedDevices()
                    }
                }

                LaunchedEffect(hasPermissions) {
                    if (hasPermissions) {
                        viewModel.scanPairedDevices()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainControllerScreen(
                        viewModel = viewModel,
                        hasPermissions = hasPermissions,
                        onRequestPermissions = {
                            permissionLauncher.launch(permissionsToRequest)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh paired devices list whenever the app is brought forward
        viewModel.scanPairedDevices()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Gracefully clean up registrations and active HID pipes
        bluetoothHidManager.unregister()
    }
}
