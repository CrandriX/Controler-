package com.example.data

import com.example.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProfileRepository(private val profileDao: ProfileDao) {

    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfilesFlow()

    suspend fun getProfileById(id: Long): Profile? {
        return profileDao.getProfileById(id)
    }

    suspend fun getDefaultProfile(): Profile? {
        return profileDao.getDefaultProfile()
    }

    suspend fun insertProfile(profile: Profile): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    suspend fun setDefaultProfile(profileId: Long) {
        profileDao.setDefaultProfile(profileId)
    }

    /**
     * Seed templates if there are no items in the DB
     */
    suspend fun seedTemplatesIfNeeded() {
        val current = allProfiles.first()
        if (current.isEmpty()) {
            // 1. Classic Gamepad
            val classic = Profile(
                name = "Classic Gamepad",
                joystickSensitivity = 1.0f,
                touchpadSensitivity = 1.2f,
                deadZone = 0.05f,
                isDefault = true
            )
            val classicId = profileDao.insertProfile(classic)

            // 2. Pro FPS Profile
            val fps = Profile(
                name = "Pro FPS Action",
                joystickSensitivity = 1.4f,
                touchpadSensitivity = 1.5f,
                deadZone = 0.03f,
                isDefault = false
            )
            profileDao.insertProfile(fps)

            // 3. Precision Navigate Touch
            val media = Profile(
                name = "Precision Trackpad",
                joystickSensitivity = 0.8f,
                touchpadSensitivity = 1.8f,
                deadZone = 0.08f,
                isDefault = false
            )
            profileDao.insertProfile(media)
        }
    }
}
