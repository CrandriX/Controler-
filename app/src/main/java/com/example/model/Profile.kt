package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "controller_profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val joystickSensitivity: Float = 1.0f,
    val touchpadSensitivity: Float = 1.2f,
    val deadZone: Float = 0.05f,
    val swapJoysticks: Boolean = false,
    val buttonMapA: Int = 1,      // Button indices 1 to 16 matching report descriptor elements
    val buttonMapB: Int = 2,
    val buttonMapX: Int = 3,
    val buttonMapY: Int = 4,
    val buttonMapL1: Int = 5,
    val buttonMapR1: Int = 6,
    val buttonMapL2: Int = 7,
    val buttonMapR2: Int = 8,
    val buttonMapSelect: Int = 9,
    val buttonMapStart: Int = 10,
    val buttonMapL3: Int = 11,
    val buttonMapR3: Int = 12,
    val buttonMapGuide: Int = 13,
    val isDefault: Boolean = false
)
